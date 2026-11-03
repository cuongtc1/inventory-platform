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
- The client sends the token back as `Authorization: Bearer <accessToken>`. Checking it is the api-gateway's job (T3.3); `GET /api/auth/me`, users and passwords come in T3.2.

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

Passwords are never stored, only their BCrypt hash (`$2a$10$` + 22 characters of salt + 31 of hash). Users are deactivated, not deleted (T3.2), so their email stays meaningful in the audit columns of other data.

## Code

| Path | Purpose |
|---|---|
| `controller/AuthController` | `POST /api/auth/login` |
| `service/AuthService` | Finds the user by email, checks password and status, builds the response |
| `service/TokenService` | Creates the signed JWT with the claims above, times from the `Clock` bean |
| `config/SecurityConfig` | `PasswordEncoder` (BCrypt) and `JwtEncoder` (HS256 with `JWT_SECRET`) beans |
| `config/JwtProperties`, `config/AdminProperties` | Typed settings from `application.yml` |
| `config/AdminAccountInitializer` | Creates the first ADMIN at startup when there are no users |
| `entity/User`, `repository/UserRepository` | The `users` table |
| `constant/UserType`, `constant/UserStatus`, `constant/MessageKeys` | Enums and message keys |

## Tests

`./mvnw test`: 30 tests, Docker must be running.

| Kind | Classes | What they check |
|---|---|---|
| Unit | `service/AuthServiceTest` | Login success, email normalization, wrong password, unknown email, deactivated user, a 216-byte password (all 401, never a 500) |
| Unit | `service/TokenServiceTest` | Real tokens decoded by Spring Security's `NimbusJwtDecoder`: claims and expiry; another secret and a changed role are rejected |
| Unit | `config/AdminAccountInitializerTest`, `config/JwtPropertiesTest` | Admin created only on an empty table, password hashed; secrets under 32 bytes rejected |
| Unit | `constant/MessagesPropertiesTest` | messages.properties and `MessageKeys` in step (same checks as product-service) |
| Web slice | `controller/AuthControllerTest` | JSON shape (no hash in it), 400 validation, 401 format, 405 |
| Repository slice (MySQL) | `repository/UserRepositoryTest` | Mapping of every column, lookup by email, unique email |
| Integration (MySQL) | `AuthIntegrationTest`, `AuthServiceApplicationTests` | Seeded admin logs in over HTTP, the token decodes with `JWT_SECRET`; wrong password and unknown email give the same 401 |

## New compared to Spring Boot 2.x / Java 8–11

| Old | Now | Notes |
|---|---|---|
| `spring-boot-starter-security` for everything, or jjwt for tokens | Two Spring Security modules: `spring-security-crypto` (BCrypt) and `spring-security-oauth2-jose` (`JwtEncoder`, built on Nimbus) | Login only needs to hash and sign. The full starter, which locks every endpoint behind a filter, comes when a request must carry a token (T3.2) |
| `Jwts.builder().signWith(key)...` (jjwt) | `JwtClaimsSet.builder()` + `JwtEncoder.encode(JwtEncoderParameters.from(header, claims))` | The same encoder/decoder pair the gateway's resource-server support uses |
| `new NimbusJwtEncoder(new ImmutableSecret<>(key))` | `NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build()` | The builder style of Spring Security 7 (7.1.1 here) |
| `@ConfigurationProperties` class with getters and setters, `@EnableConfigurationProperties` | A `record` + `@ConfigurationPropertiesScan` on the application class | Constructor binding is automatic for records (Boot 3); the compact constructor checks the secret's length |
| `CommandLineRunner` with `String... args` | `ApplicationRunner` with `ApplicationArguments` | Both exist; `ApplicationRunner` gets parsed arguments |
