# stockline-common

Shared library for all Stockline services: the code that would otherwise be copied into every service. A plain jar (no `main`, not runnable), installed into the local Maven repository and used by services as a normal dependency.

## Build and install

Services can't compile until the library is in `~/.m2`. Install it first, and again after every change to it:

```bash
cd libs/stockline-common
./mvnw install        # Windows: mvnw.cmd install. Runs the library's tests, then copies the jar to ~/.m2
```

On Windows, `install` fails with `Failed to install artifact ... .jar.<number>.tmp -> ... .jar` when another program has the old jar open: a running service, or an IDE's Java tools (Spring Tools / Eclipse, sometimes VS Code's Java extension). Stop the service or close that IDE, then run `install` again.

Then in a service's `pom.xml`:

```xml
<dependency>
	<groupId>com.stockline</groupId>
	<artifactId>stockline-common</artifactId>
	<version>${stockline-common.version}</version>   <!-- 0.0.1-SNAPSHOT -->
</dependency>
```

and in its `application.yml`:

```yaml
spring:
  messages:
    basename: messages,common-messages
```

## What's inside

| Package | Classes | Purpose |
|---|---|---|
| `constant` | `ErrorCode`, `CommonMessageKeys`, `AuditConstants`, `JwtClaimNames`, `UserType` | Error codes with HTTP status, keys of `common-messages.properties`, the `system` user |
| `exception` | `ApiException`, `ResourceNotFoundException`, `DuplicateResourceException`, `ResourceInUseException`, `InvalidRequestException`, `UnauthorizedException`, `ForbiddenException`, `InsufficientStockException`, `FieldValidationError`, `GlobalExceptionHandler`, `DataExceptionHandler` | The JSON error format (docs/conventions.md section 3) for every service. `DataExceptionHandler` holds the Hibernate and Spring Data errors (constraints, unknown sort field, `@Version` conflicts), so a service without a database (the gateway) can load the rest |
| `security` | `JwtSecurity`, `JwtCheckProperties`, `JwtAuditorAware` | What checking a JWT has in common everywhere: key, `JwtDecoder`, `role` claim, role hierarchy, shared `HttpSecurity` settings, 401/403 answers, the auditor from the token (docs/conventions.md section 11). Needs the resource-server starter in the service |
| `entity` | `BaseEntity` | `id` and the four audit columns; services' entities extend it |
| `dto` | `PageResponse` | The list format (docs/conventions.md section 4) |
| `util` | `DateTimeUtils`, `MoneyUtils`, `CommonValidationUtils`, `TextUtils` | Asia/Ho_Chi_Minh date ranges, USD with 2 decimals, shared validation rules, text cleanup, LIKE patterns for search (`containsPattern`) |
| `autoconfigure` | `CommonTimeAutoConfiguration`, `CommonJpaAutoConfiguration`, `CommonWebAutoConfiguration` | Create the beans below in every service automatically |
| resources | `common-messages.properties` | Shared texts: validation, request and resource errors, server errors, log texts |

### What a service gets automatically

| Bean | Condition | Replace it by defining your own |
|---|---|---|
| `Clock` (UTC) | always | a `Clock` bean (tests use a fixed or movable clock) |
| JPA auditing (`auditDateTimeProvider`, `auditorAware`: the email in the token with Spring Security, else `system`) | Spring Data JPA on the classpath | a bean named `auditorAware` |
| `JwtDecoder` (from `stockline.jwt.secret` and `.issuer`), `RoleHierarchy` | the resource-server starter on the classpath | a bean of that type |
| `GlobalExceptionHandler` | servlet web application | a `GlobalExceptionHandler` bean |
| `DataExceptionHandler` | servlet web application with Hibernate and Spring Data | a `DataExceptionHandler` bean |

## How auto-configuration works

Services scan only their own package (`com.stockline.product`), so they never find `com.stockline.common` classes by scanning. Instead, Spring Boot reads

```
src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

in every jar on the classpath and applies the `@AutoConfiguration` classes listed there. Test slices only load their own lists, so the library also has:

| File (in `META-INF/spring/`) | Used by | Lists |
|---|---|---|
| `org.springframework.boot.autoconfigure.AutoConfiguration.imports` | the running app, `@SpringBootTest` | time, JPA, web |
| `org.springframework.boot.data.jpa.test.autoconfigure.AutoConfigureDataJpa.imports` | `@DataJpaTest` | time, JPA (auditing fills the NOT NULL audit columns) |
| `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureWebMvc.imports` | `@WebMvcTest` | time, web (the error handler) |

This is exactly how Spring Boot's own starters work.

## Rules

- Only cross-cutting code goes here: errors, messages, time, money, validation, auditing, paging. **Never** domain code (entities, services, DTOs of a resource), and never anything that makes one service depend on another's data.
- The web and JPA starters are `<optional>` dependencies: the library compiles against them, but a service only gets them if it adds them itself.
- A change here changes every service. Run the library's tests, `./mvnw install`, then the tests of every service that uses it.
- Version: `0.0.1-SNAPSHOT` for now. "SNAPSHOT" means "still changing": Maven picks up a newly installed jar with the same version.

## Tests

`./mvnw test` (no Docker needed): utils, `common-messages.properties` checks, and `CommonAutoConfigurationTest`, which uses `ApplicationContextRunner` to check which beans each auto-configuration creates. The handler and auditing are also tested with real controllers and MySQL in product-service.
