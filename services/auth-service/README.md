# auth-service

Users and login for Stockline: BCrypt passwords and JWT tokens. Spring Boot 4.1, Java 21, port **8081**, database schema `auth_db`. Built like [product-service](../product-service/README.md) (same layers, error format, tests and Docker setup); this README covers what is different.

## Run

Start the infrastructure first (from `infra/`): `docker compose up -d`. Install the shared library once, and again after every change to it: `cd libs/stockline-common` then `./mvnw install`.

From this folder:

```bash
./mvnw spring-boot:run      # Windows cmd/PowerShell: mvnw.cmd spring-boot:run
./mvnw test                 # needs Docker running (Testcontainers)
./mvnw clean package
```

| URL | What |
|---|---|
| http://localhost:8081/actuator/health | Health check |
| http://localhost:8081/swagger-ui.html | Swagger UI |

As a container (from `infra/`): `docker compose --profile services up -d --build auth-service`. Settings come from `infra/.env` (see `infra/.env.example`).

## The first login

On startup, when `auth_db.users` is empty, the service creates one ADMIN account from `ADMIN_EMAIL` / `ADMIN_PASSWORD` and logs `created the first ADMIN account ...` (never the password). Locally, without those variables:

| Email | Password |
|---|---|
| `admin@stockline.local` | `admin12345` |

Once any user exists, nothing is created or changed: a new `ADMIN_PASSWORD` doesn't change an existing account. To start over locally, empty the table (or `docker compose down -v` in `infra/`, which wipes every schema).

## API

### `POST /api/auth/login`

```json
{ "email": "admin@stockline.local", "password": "admin12345" }
```

`200`:

```json
{
  "accessToken": "eyJraWQiOi...",
  "tokenType": "Bearer",
  "expiresAt": "2026-11-03T15:58:03.607294Z",
  "user": { "id": 1, "email": "admin@stockline.local", "fullName": "Stockline Admin", "userType": "ADMIN" }
}
```

| Situation | Status | `code` |
|---|---|---|
| Email or password blank, email longer than 150, password longer than 72 | 400 | `VALIDATION_FAILED` |
| Unknown email, wrong password, or deactivated user | 401 | `UNAUTHORIZED`, always "Wrong email or password" |

- The email is trimmed and compared in lower case: `" Admin@Stockline.LOCAL "` works.
- All three login failures give the same answer **and take the same time** (an unknown email is still checked against a dummy BCrypt hash), so nobody can find out which emails exist.
- The client sends the token back as `Authorization: Bearer <accessToken>` with every other request.

### Who may call what

Every endpoint except login, health and the API docs needs a valid token. Spring Security checks it in a filter before any controller runs (`WebSecurityConfig`): signature (`JWT_SECRET`, HS256), expiry, and `iss` = `stockline-auth`.

| Endpoints | Who |
|---|---|
| `POST /api/auth/login`, `/actuator/health`, `/swagger-ui.html`, `/v3/api-docs` | Anyone |
| `GET /api/auth/me`, `PUT /api/auth/password` | Any signed-in user |
| `/api/users/**` | ADMIN only |

| Situation | Status | `code` |
|---|---|---|
| No token, a broken or wrongly signed one, or an expired one | 401 | `UNAUTHORIZED`, "Please log in (the token is missing, invalid or expired)" |
| Valid token, but not ADMIN on `/api/users` | 403 | `FORBIDDEN`, "You don't have permission to do this" |

Both use the usual error JSON: the security filter hands the problem to the library's `GlobalExceptionHandler` (`UnauthorizedException`, `ForbiddenException`), so the frontend handles every error the same way.

In Swagger UI: call login, copy `accessToken`, click **Authorize**, paste it. Every request then carries the token.

### `GET /api/auth/me`

`200` with the signed-in user (from the database, not the token):

```json
{ "id": 1, "email": "admin@stockline.local", "fullName": "Stockline Admin", "userType": "ADMIN", "status": "ACTIVE",
  "createdAt": "...", "createdBy": "system", "updatedAt": "...", "updatedBy": "system" }
```

`401` if the account was deactivated after the token was issued.

### `PUT /api/auth/password`

```json
{ "currentPassword": "admin12345", "newPassword": "a-new-password" }
```

`204`. `400 BAD_REQUEST` "The current password is wrong"; `400 VALIDATION_FAILED` if the new password has fewer than 8 or more than 72 characters. Tokens issued before the change keep working until they expire.

### Users: `/api/users` (ADMIN)

| Method and path | Result | Errors |
|---|---|---|
| `GET /api/users?search=&userType=&status=&page=0&size=20&sort=fullName,asc` | `200`, a page of users. `search` matches part of the email or name; `userType` and `status` filter exactly | `400` unknown sort field or filter value |
| `GET /api/users/{id}` | `200`, one user | `404` |
| `POST /api/users` | `201`, `Location: /api/users/{id}`; the user is ACTIVE | `400` validation, `409 DUPLICATE` email |
| `PUT /api/users/{id}` | `200`, the updated user | `400`, `404`, `409 DUPLICATE` email; `400 BAD_REQUEST` if you take ADMIN away from yourself |
| `POST /api/users/{id}/deactivate` | `200`, the user with `status: INACTIVE`; they can't log in any more | `404`; `400 BAD_REQUEST` for your own account |
| `POST /api/users/{id}/activate` | `200`, `status: ACTIVE` again; the old password still works | `404` |

Create: `{ "email", "fullName", "userType", "password" }`. Update: `{ "email", "fullName", "userType" }` (no password, no status). Response: like `/me`, never the password hash.

| Field | Rules |
|---|---|
| `email` | Required, valid email, max 150, unique; stored trimmed and in lower case |
| `fullName` | Required, max 100, trimmed |
| `userType` | `STAFF`, `MANAGER` or `ADMIN` |
| `password` (create only) | 8 to 72 characters |

- Users are never deleted, only deactivated, so their email stays meaningful in `created_by` / `updated_by` everywhere.
- **Deactivating doesn't end existing tokens.** A JWT is checked without asking the database, so a deactivated user's token keeps working on other services until it expires (at most `JWT_EXPIRATION`). auth-service itself refuses `/me` and password changes for them.
- The two "self" rules keep an ADMIN from locking themselves out. There is always at least one active ADMIN, since only an active ADMIN can deactivate or demote someone, and never themselves.
- `created_by` / `updated_by` of a user record are the email of the ADMIN who made the change (`AuditorConfig` reads it from the token); `system` for the seeded admin.

### The token

A JWT signed with HS256 and `JWT_SECRET`. Its claims (names in `JwtClaimNames` of stockline-common, rules in [docs/conventions.md](../../docs/conventions.md) section 11):

```json
{ "iss": "stockline-auth", "sub": "1", "email": "admin@stockline.local", "name": "Stockline Admin",
  "role": "ADMIN", "iat": 1793692683, "exp": 1793721483 }
```

| Claim | Meaning |
|---|---|
| `sub` | The user's id (a string, as the JWT standard wants) |
| `role` | `STAFF`, `MANAGER` or `ADMIN` |
| `email`, `name` | Login email (used for audit columns later) and display name |
| `iss` | Always `stockline-auth` |
| `iat`, `exp` | Issued at / expires at, seconds since 1970 (UTC). `exp` = `iat` + `JWT_EXPIRATION` |

Anyone can read a token's claims (paste one into a JWT debugger): they are only Base64, not encrypted, so a token never carries secrets. What protects it is the signature: changing `role` from `STAFF` to `ADMIN` makes the signature wrong, and the token is rejected (`TokenServiceTest` checks exactly that). The header also has a `kid` (key id), which Spring Security derives from the key; it doesn't matter for checking.

There is no refresh token: after `exp` the user logs in again.

## Configuration

| Variable | Default (local only) | Meaning |
|---|---|---|
| `SERVER_PORT` | `8081` | |
| `DB_URL` | `jdbc:mysql://localhost:3306/auth_db` | |
| `DB_USERNAME` / `DB_PASSWORD` | `stockline` / `stockline` | |
| `JWT_SECRET` | `local-dev-only-stockline-jwt-secret-change-me` | HMAC key, **at least 32 bytes** (the service refuses to start otherwise). The api-gateway must use the same value |
| `JWT_EXPIRATION` | `8h` | Token lifetime: `30m`, `8h`, `1d`, ... |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` / `ADMIN_FULL_NAME` | `admin@stockline.local` / `admin12345` / `Stockline Admin` | The first ADMIN (see above) |

`stockline.jwt.*` and `stockline.admin.*` in `application.yml` are read into the records `JwtProperties` and `AdminProperties`, checked at startup (`@Validated`).

## Database

| Migration | Table | Columns (besides `id` and the audit columns) |
|---|---|---|
| `V1__create_users.sql` | `users` | `email` (unique, lower case), `password_hash` (BCrypt, 60 characters), `full_name`, `user_type` (`STAFF` / `MANAGER` / `ADMIN`), `status` (`ACTIVE` / `INACTIVE`) |

Passwords are never stored, only their BCrypt hash (`$2a$10$` + 22 characters of salt + 31 of hash). Users are deactivated, not deleted, so their email stays meaningful in the audit columns of other data.

## Code

| Path | Purpose |
|---|---|
| `controller/AuthController` | `POST /api/auth/login`, `GET /api/auth/me`, `PUT /api/auth/password` |
| `controller/UserController` | `/api/users` (ADMIN) |
| `controller/CurrentUser` | The user id from the request's token (`sub`) |
| `service/AuthService` | Login, the signed-in user, changing the own password |
| `service/UserService` | List, create, update, deactivate, activate users; the two "self" rules |
| `service/TokenService` | Creates the signed JWT with the claims above, times from the `Clock` bean |
| `config/SecurityConfig` | `PasswordEncoder` (BCrypt), `JwtEncoder` and `JwtDecoder` (HS256 with `JWT_SECRET`) beans |
| `config/WebSecurityConfig` | The security filter chain: which URL needs which role, `role` claim → `ROLE_...`, 401/403 in our error format |
| `config/AuditorConfig` | `created_by` / `updated_by` = the email from the token |
| `config/OpenApiConfig` | The "Authorize" button in Swagger UI |
| `config/JwtProperties`, `config/AdminProperties` | Typed settings from `application.yml` |
| `config/AdminAccountInitializer` | Creates the first ADMIN at startup when there are no users |
| `entity/User`, `repository/UserRepository`, `repository/UserSpecifications` | The `users` table and the list filters |
| `dto/`, `mapper/UserMapper` | Request and response records; entity → response (never the hash) |
| `constant/UserType`, `UserStatus`, `PasswordPolicy`, `MessageKeys` | Enums, password lengths, message keys |

## Tests

`./mvnw test`: 66 tests, Docker must be running.

| Kind | Classes | What they check |
|---|---|---|
| Unit | `service/AuthServiceTest` | Login success and every failure (all 401, never a 500, also for a 216-byte password); `/me` for a deactivated user; password change with a right and a wrong current password |
| Unit | `service/UserServiceTest` | Create (normalized email, hashed password, ACTIVE), duplicate emails, update, the two "self" rules, deactivate, activate |
| Unit | `service/TokenServiceTest` | Real tokens decoded by Spring Security's `NimbusJwtDecoder`: claims and expiry; another secret and a changed role are rejected |
| Unit | `config/AdminAccountInitializerTest`, `config/JwtPropertiesTest` | Admin created only on an empty table, password hashed; secrets under 32 bytes rejected |
| Unit | `constant/MessagesPropertiesTest` | messages.properties and `MessageKeys` in step (same checks as product-service) |
| Web slice + security | `controller/AuthControllerTest`, `controller/UserControllerTest` | With the real `WebSecurityConfig`: JSON shape (no hash), validation, 401 without or with a broken token, 403 for MANAGER and STAFF on `/api/users`, actor id passed from the token. `TestTokens.as(id, role)` signs a request in as a user |
| Repository slice (MySQL) | `repository/UserRepositoryTest` | Every column, lookup by email, unique email, the list filters (search in email or name, `_` as plain text) |
| Integration (MySQL) | `AuthIntegrationTest`, `AuthServiceApplicationTests` | Real tokens through the real filters: admin creates a staff user, staff gets 403 on `/api/users`, changes their password, is promoted, deactivated (login and `/me` refused), activated again; audit columns name the admin; an admin can't lock themselves out; an expired token gets 401 |

## New compared to Spring Boot 2.x / Java 8–11

| Old | Now | Notes |
|---|---|---|
| `spring-boot-starter-oauth2-resource-server` | `spring-boot-starter-security-oauth2-resource-server` (+ `...-test`) | Boot 4 renamed the security starters to `spring-boot-starter-security-*`. It brings Spring Security, BCrypt and `JwtEncoder`/`JwtDecoder` (Nimbus) |
| `class SecurityConfig extends WebSecurityConfigurerAdapter` with `configure(HttpSecurity)` | A `SecurityFilterChain` `@Bean` | The adapter was removed in Spring Security 6 |
| `http.csrf().disable().authorizeRequests().antMatchers(...)` | `http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.requestMatchers(...))` | Only the lambda style exists since Spring Security 7; `antMatchers`/`mvcMatchers` became `requestMatchers` |
| `@MockBean` user + `@WithMockUser` | `.with(jwt().jwt(...).authorities(...))` from spring-security-test | A request "with a token" without signing one (`TestTokens`) |
| Custom JWT filter (`OncePerRequestFilter`) parsing the header | `oauth2ResourceServer(o -> o.jwt(...))` | Spring Security reads `Authorization: Bearer`, checks the token with the `JwtDecoder` bean and puts it into the `SecurityContext` |
| `jjwt` `Jwts.parser()` | `NimbusJwtDecoder.withSecretKey(key).macAlgorithm(HS256)` + `setJwtValidator(...)` | Expiry checked against our `Clock`, issuer checked too |
| `Jwts.builder().signWith(key)...` (jjwt) | `JwtClaimsSet.builder()` + `JwtEncoder.encode(JwtEncoderParameters.from(header, claims))` | The same encoder/decoder pair the gateway's resource-server support uses |
| `new NimbusJwtEncoder(new ImmutableSecret<>(key))` | `NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build()` | The builder style of Spring Security 7 (7.1.1 here) |
| `@ConfigurationProperties` class with getters and setters, `@EnableConfigurationProperties` | A `record` + `@ConfigurationPropertiesScan` on the application class | Constructor binding is automatic for records (Boot 3); the compact constructor checks the secret's length |
| `CommandLineRunner` with `String... args` | `ApplicationRunner` with `ApplicationArguments` | Both exist; `ApplicationRunner` gets parsed arguments |
