# Conventions

Rules that every service and the frontend follow, so all parts of Stockline look and behave the same. If a rule here doesn't fit a case, discuss it and update this file rather than working around it.

Contents:
1. [Java packages and class names](#1-java-packages-and-class-names)
2. [REST API style](#2-rest-api-style)
3. [JSON error format](#3-json-error-format)
4. [Pagination, sorting and filtering](#4-pagination-sorting-and-filtering)
5. [Database and Flyway](#5-database-and-flyway)
6. [Kafka events](#6-kafka-events)
7. [Configuration](#7-configuration)
8. [Tests](#8-tests)
9. [Git: branches and commits](#9-git-branches-and-commits)

---

## 1. Java packages and class names

Base package: `com.stockline.<service>`, where `<service>` is the service name without `-service`: `auth`, `product`, `inventory`, `order`, `notification`, `gateway`.

```
com.stockline.product
├── ProductServiceApplication.java   main class
├── controller/    REST endpoints, no business logic
├── service/       business logic and transactions
├── repository/    Spring Data JPA interfaces
├── entity/        JPA entities (map to tables)
├── dto/           request and response objects (Java records)
├── mapper/        entity <-> DTO conversion
├── exception/     custom exceptions and the global exception handler
├── message/       MessageKeys: constants for the keys in messages.properties
├── config/        @Configuration classes
├── client/        REST clients to other services (only if needed)
└── event/         Kafka event classes, producers, consumers (only if needed)
```

The flow is always `controller → service → repository`. A controller never calls a repository directly, and an entity never leaves the service layer.

| Kind | Name pattern | Example |
|---|---|---|
| Entity | singular noun | `Product`, `StockLevel` |
| Repository | `<Entity>Repository` | `ProductRepository` |
| Service | `<Entity>Service` (a class; add an interface only if there are two implementations) | `ProductService` |
| Controller | `<Entity>Controller` | `ProductController` |
| Request DTO | `<Entity>Request`, or `<Action><Entity>Request` when create and update differ | `ProductRequest`, `AdjustStockRequest` |
| Response DTO | `<Entity>Response` | `ProductResponse` |
| Mapper | `<Entity>Mapper` | `ProductMapper` |
| Exception | `<Problem>Exception` | `ResourceNotFoundException`, `DuplicateResourceException` |
| Kafka event | `<Thing><PastTenseVerb>Event` | `OrderCreatedEvent`, `StockReservedEvent` |

Code style:
- Indent Java files with **tabs**, one tab per level. Wrapped (continuation) lines get two extra tabs.
- DTOs are Java `record`s with Bean Validation annotations, for example `@NotBlank`, `@Size` and `@Positive`.
- Use constructor injection with `final` fields. Don't put `@Autowired` on fields.
- Mappers are small hand-written classes (`@Component`), not MapStruct. They're easy to read and debug.
- No Lombok. Entities have normal getters and setters (generate them in the IDE) and a protected no-args constructor for JPA.
- Money is `BigDecimal`, never `double`. Time is `Instant`, always UTC.
- Enums are stored as strings (`@Enumerated(EnumType.STRING)`).

Messages:
- Every text the API returns, and log texts, live in `src/main/resources/messages.properties`, one key per message, never as string literals in Java.
- Sections: `common` (validation, request, resource and server errors used everywhere), one section per resource (`category`, `supplier`, `product`, ...), and `system` (log texts, never sent to the client).
- Key format: `<section>.<subject>.<problem>`, lower case, words joined with `-`: `common.resource.not-found`, `category.delete.has-products`, `system.error.unexpected`.
- Java code uses the constants in `message/MessageKeys`. Exceptions carry a key and arguments (`new DuplicateResourceException(MessageKeys.CATEGORY_NAME_DUPLICATE, name)`); the `GlobalExceptionHandler` looks up the text.
- Bean Validation annotations reference a key in braces: `@Pattern(message = "{product.sku.invalid}")`. The file also overrides the default texts (`jakarta.validation.constraints.NotBlank.message`, ...), and Spring MVC's own texts (`problemDetail.<exception class>`).
- Placeholders are `{0}`, `{1}` (`java.text.MessageFormat`). In a message with placeholders, write an apostrophe as `''`: `Category ''{0}'' already exists`. Pass numbers as text, or MessageFormat prints `1234` as `1,234` (the handler does this).

## 2. REST API style

### URLs

- Every endpoint starts with `/api`. The gateway routes by the first segment after `/api`, so a service uses the same path the frontend calls, and the gateway forwards it unchanged.
- Resources are **plural nouns in kebab-case**: `/api/products`, `/api/stock-levels`, `/api/sales-orders`.
- IDs go in the path: `/api/products/{id}`.
- Nest a resource only when it can't exist without its parent: `/api/sales-orders/{id}/items`, `/api/sales-orders/{id}/history`.
- Actions that are not plain create, read, update or delete use `POST` with a verb at the end:
  `POST /api/sales-orders/{id}/place`, `/pay`, `/ship`, `/cancel`, and `POST /api/purchase-orders/{id}/receive`.

| Service | Base paths |
|---|---|
| auth-service | `/api/auth` (login, me, password), `/api/users` |
| product-service | `/api/categories`, `/api/suppliers`, `/api/products` |
| inventory-service | `/api/warehouses`, `/api/stock-levels`, `/api/stock-movements` |
| order-service | `/api/customers`, `/api/sales-orders`, `/api/purchase-orders` |
| notification-service | `/api/notifications` |

### Methods and status codes

| Operation | Request | Success response |
|---|---|---|
| List | `GET /api/products?page=0&size=20` | `200` with a page object (section 4) |
| Get one | `GET /api/products/{id}` | `200` with the resource |
| Create | `POST /api/products` | `201` with the created resource and a `Location: /api/products/{id}` header |
| Update | `PUT /api/products/{id}` (full replace of editable fields) | `200` with the updated resource |
| Delete | `DELETE /api/products/{id}` | `204`, no body |
| Action | `POST /api/sales-orders/{id}/ship` | `200` with the updated resource |

We don't use `PATCH`; `PUT` with the full form is simpler for a CRUD UI.

Error status codes: `400` invalid input, `401` missing or invalid token, `403` role not allowed, `404` not found, `409` conflict (duplicate value, invalid status change, record still in use, optimistic-lock failure), `500` unexpected error. Section 3 shows the error body.

### JSON bodies

- Field names are `camelCase`: `categoryId`, `reorderLevel`, `createdAt`.
- IDs are numbers (`BIGINT` in the database).
- Enums are upper-case strings: `"ACTIVE"`, `"PENDING"`, `"MANAGER"`.
- Timestamps are ISO-8601 in UTC: `"2026-10-22T08:15:30Z"`. Dates without a time use `"2026-10-22"`.
- Money and quantities with decimals are JSON numbers: `"price": 12.50`.
- A response includes only what the client needs, never password hashes or internal fields.

Example `GET /api/products/12`:

```json
{
  "id": 12,
  "sku": "KB-104",
  "name": "Mechanical Keyboard",
  "categoryId": 3,
  "categoryName": "Accessories",
  "supplierId": 5,
  "supplierName": "Acme Supplies",
  "price": 59.90,
  "status": "ACTIVE",
  "createdAt": "2026-10-22T08:15:30Z",
  "updatedAt": "2026-10-22T08:15:30Z"
}
```

## 3. JSON error format

Every error from every service uses the **Problem Details** format (RFC 9457) with content type `application/problem+json`. Spring Boot supports it out of the box with the `ProblemDetail` class. Each service has one `@RestControllerAdvice` (`GlobalExceptionHandler` in the `exception` package) that builds it.

| Field | Meaning |
|---|---|
| `type` | Always `"about:blank"` (we don't publish error docs pages) |
| `title` | Short text for the HTTP status, e.g. `"Not Found"` |
| `status` | HTTP status code |
| `detail` | Human-readable message the frontend can show to the user |
| `instance` | Request path |
| `code` | Stable, machine-readable error code; the frontend checks this, not `detail` |
| `timestamp` | When the error happened (UTC) |
| `errors` | Only for validation errors: one entry per invalid field |

Validation error (`400`):

```json
{
  "type": "about:blank",
  "title": "Bad Request",
  "status": 400,
  "detail": "Validation failed",
  "instance": "/api/products",
  "code": "VALIDATION_FAILED",
  "timestamp": "2026-10-22T08:15:30Z",
  "errors": [
    { "field": "sku", "message": "must not be blank" },
    { "field": "price", "message": "must be greater than 0" }
  ]
}
```

Not found (`404`):

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "Product with id 99 not found",
  "instance": "/api/products/99",
  "code": "NOT_FOUND",
  "timestamp": "2026-10-22T08:15:30Z"
}
```

Error codes:

| Code | Status | When |
|---|---|---|
| `VALIDATION_FAILED` | 400 | Bean Validation failed, or a body field or filter parameter has a value of the wrong type, e.g. `"status": "SOLD_OUT"` or `"categoryId": "abc"` (`errors` lists the fields) |
| `BAD_REQUEST` | 400 | Other invalid input: broken or missing JSON, a wrong path variable type (`/api/products/abc`), an unknown sort field, a business rule such as a category loop |
| `UNAUTHORIZED` | 401 | No token, bad token, expired token, or wrong login |
| `FORBIDDEN` | 403 | The user's role is too low |
| `NOT_FOUND` | 404 | The resource doesn't exist, a referenced record (e.g. `categoryId`) doesn't exist, or the URL doesn't exist |
| `METHOD_NOT_ALLOWED` | 405 | The URL exists but not with this HTTP method, e.g. `PATCH` |
| `NOT_ACCEPTABLE` | 406 | The client's `Accept` header asks for a format we don't produce |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | The body isn't `application/json` |
| `DUPLICATE` | 409 | A unique value already exists, e.g. SKU or username. Found by the service check, or by the database's unique key when two requests arrive at the same moment |
| `IN_USE` | 409 | Can't delete because other records still use it. Found by the service check, or by a foreign key |
| `INVALID_STATUS_TRANSITION` | 409 | e.g. shipping an order that isn't paid |
| `CONCURRENT_UPDATE` | 409 | Optimistic-lock failure; the client should reload and retry |
| `INSUFFICIENT_STOCK` | 409 | A stock adjustment would make available stock negative |
| `INTERNAL_ERROR` | 500 | Anything unexpected. `detail` is generic; the stack trace goes only to the log |

Add new codes to this table when a task needs one. Other errors raised by Spring MVC itself use the HTTP status name as the code (e.g. `PAYLOAD_TOO_LARGE`); every 5xx is `INTERNAL_ERROR`.

How each service's `GlobalExceptionHandler` gets there (product-service is the reference):
- The service throws an `ApiException` subclass (`ResourceNotFoundException`, `DuplicateResourceException`, ...) that carries status and code.
- It extends `ResponseEntityExceptionHandler`, so Spring MVC's own errors already have a Problem Details body; `handleExceptionInternal` adds `type`, `instance`, `code` and `timestamp` to them.
- `DataIntegrityViolationException` is mapped by the constraint that failed: unique key → `DUPLICATE`, row still referenced → `IN_USE`, referenced row missing → `NOT_FOUND`. Services still check first, so the usual case gets a precise message.
- `@ExceptionHandler(Exception.class)` logs the stack trace at ERROR and returns a generic 500. Error responses never contain exception messages, SQL or stack traces.

## 4. Pagination, sorting and filtering

List endpoints return one page at a time.

**Request parameters** (Spring Data `Pageable`):

| Parameter | Default | Notes |
|---|---|---|
| `page` | `0` | Page number, **starting at 0** |
| `size` | `20` | Maximum `100` |
| `sort` | depends on resource, usually `createdAt,desc` | `field,asc` or `field,desc`; repeat for several fields: `sort=status,asc&sort=name,asc` |

Filters are extra query parameters named after the field, in camelCase:

```
GET /api/products?search=keyboard&categoryId=3&status=ACTIVE&page=0&size=20&sort=name,asc
```

- `search` is a free-text filter. Each endpoint documents which fields it searches; products search `name` and `sku`, case-insensitive, "contains".
- Date ranges use `from` and `to` (inclusive, ISO date): `?from=2026-10-01&to=2026-10-31`.
- Unknown sort fields return `400`.

**Response** is a `PageResponse<T>` record shared by all services, not Spring's own `Page` JSON:

```json
{
  "content": [ { "id": 12, "sku": "KB-104", "name": "Mechanical Keyboard" } ],
  "page": 0,
  "size": 20,
  "totalElements": 135,
  "totalPages": 7
}
```

Ant Design's table counts pages from 1, so the frontend sends `page = current - 1`.

## 5. Database and Flyway

- Tables are **plural, snake_case**: `products`, `stock_levels`, `sales_order_items`. Columns are snake_case: `reorder_level`, `created_at`.
- Primary key is `id BIGINT AUTO_INCREMENT`.
- Most tables have `created_at` and `updated_at` as `DATETIME(6)`, stored in UTC.
- Rows that can be changed by two users at once have `version BIGINT NOT NULL DEFAULT 0` (`@Version` in JPA).
- Money is `DECIMAL(12,2)`. Quantities are `INT`, or `DECIMAL` if a product can be sold in fractions.
- Enums are `VARCHAR(20)`.
- Constraint names: `pk_<table>`, `fk_<table>_<referenced_table>`, `uk_<table>_<column>`, `idx_<table>_<column>`.
- Foreign keys only **inside** one schema. A reference to another service's data is a plain column (`product_id BIGINT NOT NULL`) with an index, plus any snapshot values the service needs, e.g. `product_name` on an order item.
- Flyway files are in `src/main/resources/db/migration`, named `V<number>__<description>.sql` with a double underscore: `V1__create_categories.sql`, `V2__create_suppliers.sql`.
- A committed migration is never edited. To change a table, add a new migration.
- `spring.jpa.hibernate.ddl-auto=validate`: Flyway creates tables, Hibernate only checks that entities match them.

## 6. Kafka events

- Topic names are the ones in `TASKS.md`: `order.created`, `stock.reserved`, `stock.failed`, `order.cancelled`, `stock.low`. Format: `<entity>.<past-tense-verb>`.
- The message **key** is the ID of the main entity (for order events, the order ID). Kafka keeps messages with the same key in order.
- The message **value** is JSON with a common envelope:

```json
{
  "eventId": "6f1c2a1e-6a43-4c3e-9b8a-0d6f4b1e2c55",
  "eventType": "order.created",
  "occurredAt": "2026-10-22T08:15:30Z",
  "payload": {
    "orderId": 42,
    "items": [ { "productId": 12, "warehouseId": 1, "quantity": 2 } ]
  }
}
```

- `eventId` is a UUID. Consumers store it in `processed_events` and skip any event they have already processed, which makes them idempotent.
- The consumer group ID is the service name, e.g. `inventory-service`.
- Events are facts that already happened. Name and treat them as such; one service never tells another what to do through an event.

## 7. Configuration

- Each service has `src/main/resources/application.yml`. Values that change between environments come from environment variables, with a default that works locally:

```yaml
server:
  port: ${SERVER_PORT:8082}
spring:
  datasource:
    url: ${DB_URL:jdbc:mysql://localhost:3306/product_db}
    username: ${DB_USERNAME:stockline}
    password: ${DB_PASSWORD:stockline}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

- Standard variable names: `SERVER_PORT`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `KAFKA_BOOTSTRAP_SERVERS`, `JWT_SECRET`, and `<SERVICE>_URL` for REST clients, e.g. `PRODUCT_SERVICE_URL`.
- Real secrets are never committed. The local defaults above are only for the Docker setup on your machine.

## 8. Tests

- Test classes mirror the main class: `ProductService` → `ProductServiceTest` in the same package under `src/test/java`.
- Method names describe the case: `<method>_<condition>_<expectedResult>`, e.g. `create_duplicateSku_throwsDuplicateResourceException`.
- Each test has three parts, given / when / then, separated by blank lines or comments.

| Layer | Tool | What it checks |
|---|---|---|
| Service | JUnit 5 + Mockito (no Spring) | Business rules, with repositories mocked |
| Controller | `@WebMvcTest` + `MockMvc` | URL, status code, JSON shape, validation errors |
| Repository | `@DataJpaTest` | Custom queries and constraints |
| Integration | `@SpringBootTest` + `@AutoConfigureMockMvc` + Testcontainers | The layers together over HTTP, nothing mocked; at least one per service |
| Messages | Plain JUnit | Every `MessageKeys` constant and annotation key has a text, no unused keys, `''` in messages with placeholders |

- Assert exceptions by type, message key and arguments (`.hasMessage(MessageKeys.X).extracting("messageArgs")`), not by English text; the final text is checked in controller and integration tests.
- Tests that need a database use Testcontainers (`@Import(TestcontainersConfiguration.class)`); Docker must be running.
- Integration tests don't roll back, so each test uses its own names and SKUs.

## 9. Git: branches and commits

### Branches

- `main` always builds and runs. Nobody commits to it directly.
- One branch per task: `feature/<task-id>-<short-name>`, lower-case words joined by `-`: `feature/T1.3-category-crud`.
- Fixes outside a task: `fix/<short-name>`, e.g. `fix/product-search-case`.
- The developer reviews a branch and merges it into `main`. If a task starts before the previous one is merged, its branch starts from the previous task's branch (stacked branches); merging the later branch brings in both.

### Commits

```
T1.3: Add category CRUD API

- CategoryController with list, get, create, update, delete
- Validation for name (required, max 100 chars)
- Unit and controller tests
```

- Subject line: `<task-id>: <what the commit does>`, imperative mood ("Add", "Fix", "Update", not "Added"), at most 72 characters, no period at the end.
- Use `fix:` instead of a task id for a commit outside a task: `fix: Ignore case in product search`.
- Body (optional): a blank line, then short bullets saying what changed and why.
- One logical change per commit. Don't mix formatting changes with features.
- Before committing: the code builds, tests pass, and no secrets, `.env` files or build output are staged.
