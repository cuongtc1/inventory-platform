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
10. [Frontend](#10-frontend)

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
├── constant/      this service's enums (ProductStatus) and MessageKeys
├── config/        @Configuration classes (only if needed)
├── exception/     exceptions only this service needs (only if needed)
├── util/          helpers only this service needs (only if needed)
├── client/        REST clients to other services (only if needed)
└── event/         Kafka event classes, producers, consumers (only if needed)
```

Code every service needs lives once, in the shared library `libs/stockline-common` (`com.stockline.common`, see its README):

```
com.stockline.common
├── constant/      ErrorCode, CommonMessageKeys, AuditConstants
├── exception/     ApiException + subclasses, GlobalExceptionHandler
├── entity/        BaseEntity (id + audit columns)
├── dto/           PageResponse
├── util/          DateTimeUtils, MoneyUtils, CommonValidationUtils, TextUtils
└── autoconfigure/ Clock, JPA auditing and the exception handler, registered in every service automatically
```

Only cross-cutting code goes into the library, never domain code (entities, services or DTOs of a resource). A service gets it with one dependency in `pom.xml` and `spring.messages.basename: messages,common-messages` in `application.yml`. After changing the library: its tests, `mvnw install` in `libs/stockline-common`, then the tests of every service.

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
- Before writing a helper a second time, put it in a `util/` class: the library's if another service could need it, otherwise the service's own. Utility classes are `final`, with a private constructor and only `static` methods.
- Editor settings come from `.editorconfig` in the repo root (tabs, UTF-8, LF). IntelliJ reads it by itself; VS Code needs the "EditorConfig for VS Code" extension.

Enums:
- Enums live in `constant/`, never in `entity/` or `dto/`: `com.stockline.product.constant.ProductStatus`; shared ones in the library, `com.stockline.common.constant.ErrorCode`.
- Stored as strings (`@Enumerated(EnumType.STRING)`, column `VARCHAR(20)`), sent in JSON as the upper-case name (`"ACTIVE"`). The API returns only the name; the frontend turns it into a label ("Active").
- Behaviour that belongs to the values goes into the enum, e.g. allowed order status changes (T5.4): `OrderStatus.PAID.canChangeTo(SHIPPED)`.
- Never share a Java enum between services. Events and REST calls between services carry the name as a string.
- Error codes are the `ErrorCode` enum (each with its HTTP status), never string literals.

Dates and times (library: `com.stockline.common.util.DateTimeUtils`):
- Every moment is a `java.time.Instant` (UTC), stored as `DATETIME(6)` and sent as ISO-8601 with `Z`. No `Date`, `LocalDateTime` or time-zone strings in entities or DTOs.
- "Now" comes from the `Clock` bean (`clock.instant()`), never `Instant.now()`, so tests can fix the time.
- The business time zone is `Asia/Ho_Chi_Minh` (`DateTimeUtils.BUSINESS_ZONE`). Dates the user picks ("today", `?from=2026-10-01&to=2026-10-31`, "October") are Vietnam dates and become Instant ranges with `startOfDay` / `endOfDayExclusive` / `startOfMonth`. Ranges are half-open: `>= start AND < end`.
- Texts people read (exports, e-mails) use `DateTimeUtils.formatDate` / `formatDateTime` (`dd/MM/yyyy HH:mm:ss`, Vietnam time). The API never sends formatted dates.

Money (library: `com.stockline.common.util.MoneyUtils`):
- One currency: **USD**. Amounts are `BigDecimal` with 2 decimals, `DECIMAL(12,2)` in the database, plain JSON numbers (`12.50`). Never `double`.
- Calculate at full precision and round once at the end with `MoneyUtils.toMoney` (`HALF_UP`). Use `MoneyUtils.multiply` / `sum` for line totals and order totals.
- Request fields: `@Digits(integer = 10, fraction = 2)`; prices `@Positive`, costs `@PositiveOrZero`.

Validation (library: `com.stockline.common.util.CommonValidationUtils`):
- A rule used by more than one field (phone, code/SKU, maximum lengths) is a constant there, and annotations point to it: `@Pattern(regexp = CommonValidationUtils.PHONE_REGEX, message = CommonValidationUtils.PHONE_MESSAGE)`.
- Text cleanup before saving: `TextUtils.blankToNull` for optional fields, `strip()` for required ones.

Messages:
- Every text the API returns, and log texts, live in properties files, one key per message, never as string literals in Java. Two files per service: `common-messages.properties` in the library (sections `common` and `system`) and the service's own `src/main/resources/messages.properties` (one section per resource: `category`, `supplier`, `product`, ...). The service's file is read first, so it can override a common text.
- `system` texts are for logs only, never sent to the client.
- Key format: `<section>.<subject>.<problem>`, lower case, words joined with `-`: `common.resource.not-found`, `category.delete.has-products`, `system.error.unexpected`.
- Unique keys: a service adds `constraint.<key name>=...` (e.g. `constraint.uk_products_sku`) so a unique-key error from MySQL gets a precise text; without it the common "A record with this value already exists" is used.
- Java code uses constants: `CommonMessageKeys` (library) and the service's `constant/MessageKeys`. Exceptions carry a key and arguments (`new DuplicateResourceException(MessageKeys.CATEGORY_NAME_DUPLICATE, name)`); the `GlobalExceptionHandler` looks up the text.
- Bean Validation annotations reference a key in braces: `@Pattern(message = "{common.validation.code.invalid}")`, usually through a `CommonValidationUtils.*_MESSAGE` constant. The file also overrides the default texts (`jakarta.validation.constraints.NotBlank.message`, ...), and Spring MVC's own texts (`problemDetail.<exception class>`).
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
- Every resource response ends with the audit fields `createdAt`, `createdBy`, `updatedAt`, `updatedBy` (the user's login, or `system`).

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
  "costPrice": 40.00,
  "salePrice": 59.90,
  "reorderLevel": 20,
  "status": "ACTIVE",
  "createdAt": "2026-10-22T08:15:30Z",
  "createdBy": "system",
  "updatedAt": "2026-10-22T08:15:30Z",
  "updatedBy": "system"
}
```

## 3. JSON error format

Every error from every service uses the **Problem Details** format (RFC 9457) with content type `application/problem+json`. Spring Boot supports it out of the box with the `ProblemDetail` class. One `@RestControllerAdvice`, `GlobalExceptionHandler` from the `stockline-common` library, builds it in every service; the library registers it automatically.

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

How the library's `GlobalExceptionHandler` gets there:
- The service throws an `ApiException` subclass (`ResourceNotFoundException`, `DuplicateResourceException`, ...) that carries an `ErrorCode` (which has the HTTP status) and a message key.
- It extends `ResponseEntityExceptionHandler`, so Spring MVC's own errors already have a Problem Details body; `handleExceptionInternal` adds `type`, `instance`, `code` and `timestamp` to them.
- `DataIntegrityViolationException` is mapped by the constraint that failed: unique key → `DUPLICATE` (text from `constraint.<key name>`), row still referenced → `IN_USE`, referenced row missing → `NOT_FOUND`. Services still check first, so the usual case gets a precise message.
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
- Every table has the audit columns `created_at`, `updated_at` (`DATETIME(6)`, UTC) and `created_by`, `updated_by` (`VARCHAR(100)`, the user's login from the JWT, `system` before Phase 3 or for jobs). Entities extend `BaseEntity`; Spring Data JPA auditing fills all four, services never set them.
- Deletes are **hard deletes** (`DELETE FROM ...`), the rule for every resource. A row other rows still use can't be deleted (409 `IN_USE`). Services that keep a reference to another service's data also keep a copy of what they show (e.g. `product_name` on an order line), so history stays readable after the original is deleted.
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

### Docker

- Every service has a multi-stage `services/<name>/Dockerfile` (see product-service's): a build stage on `eclipse-temurin:21-jdk` that installs `libs/stockline-common` and packages the service with `-DskipTests` (Testcontainers can't run inside `docker build`; tests run before every commit), and a runtime stage on `eclipse-temurin:21-jre` with the layers from `-Djarmode=tools extract --layers --launcher`.
- The build context is the **repo root**, so the image can build the library. The root `.dockerignore` sends only `libs/` and `services/` (without `target/`, IDE files, `.env`).
- The container runs as a non-root user, sets `TZ=UTC` and `-XX:MaxRAMPercentage=75`, and uses an exec-form `ENTRYPOINT`.
- Each service is added to `infra/docker-compose.yml` under `profiles: ["services"]`, with `container_name: stockline-<name>`, `depends_on` on the healthy infrastructure it needs, a healthcheck on `/actuator/health`, and a host port `${<NAME>_PORT:-<port>}` (also in `.env.example`).
- A container is configured only through environment variables (the standard names above). Inside the compose network the hosts are `mysql:3306` and `kafka:29092`, never `localhost`. No environment-specific configuration (hosts, passwords, `.env`) is baked into the image; it comes only from environment variables. Maven cache mounts use `sharing=locked`, because several service images may build at the same time.

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
| Messages | Plain JUnit | Every `MessageKeys` constant and annotation key has a text, no unused keys, `''` in messages with placeholders, every `constraint.*` key names a real unique key |
| Library | Plain JUnit, `ApplicationContextRunner` | Utils, `common-messages.properties`, which beans each auto-configuration creates (in `libs/stockline-common`) |

- Assert exceptions by type, message key and arguments (`.hasMessage(MessageKeys.X).extracting("messageArgs")`), not by English text; the final text is checked in controller and integration tests.
- Tests that need a database use Testcontainers (`@Import(TestcontainersConfiguration.class)`); Docker must be running.
- Integration tests don't roll back, so each test uses its own names and SKUs.
- Build test entities with `TestData` (`src/test/java/.../TestData.java`, static import), not with a private helper per test class.
- Tests that need a fixed or moving time replace the `Clock` bean (see `AuditingTest`).
- `@WebMvcTest` and `@DataJpaTest` get the library's Clock, auditing and exception handler automatically; no `@Import` of config classes needed.

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

## 10. Frontend

The React app in `frontend/`. The same ideas as the backend apply: one place for each concern, no hard-coded texts or formats scattered over pages, and rules settled before the second copy appears.

### Language and tooling

- **TypeScript** with `"strict": true` (Vite's `react-ts` template). Components are `.tsx`, everything else `.ts`. No `any`: describe the data with a type, or use `unknown` and narrow it.
- Vite, React, Ant Design, React Router and Axios at their latest stable versions when the project was created (T2.0), pinned through `package-lock.json` (committed, lockfile version 3). Node 20 or newer, **npm 10 or newer**.
- No TypeScript `enum`, `namespace` or constructor parameter properties: `tsconfig` uses `erasableSyntaxOnly` (only syntax that can simply be deleted to get JavaScript). Use union types (`'ACTIVE' | 'INACTIVE'`) and plain fields.
- Colours come from `theme/theme.ts` (Ant Design tokens, badge tones), never written into a page.
- **ESLint** from the Vite template; `npm run lint` and `npm run build` (type check + build) must pass before committing. Indentation and line endings come from `.editorconfig` (2 spaces, LF). No Prettier or other tools without asking first.
- Nothing beyond the stack (no Redux, React Query, i18n or date libraries) without asking. State is `useState` / `useEffect` plus small custom hooks.

### Folder structure

```
frontend/src/
├── main.tsx            entry point: providers (Router, Ant Design ConfigProvider + App), renders <App />
├── App.tsx             the routes
├── api/                talking to the backend
│   ├── client.ts       the one Axios instance (base URL, every error → ApiError)
│   └── products.ts     one file per resource: getProducts, getProduct, createProduct, ...
├── types/              TypeScript types that mirror the backend DTOs: api.ts (PageResponse, ProblemDetail, ErrorCode),
│                       category.ts, supplier.ts, product.ts, ...; ui.ts (EnumDisplay)
├── pages/              one folder per area: products/ProductListPage.tsx, products/ProductFormPage.tsx
├── components/         shared building blocks: StatusBadge, DeleteButton, ...
├── layouts/            AppLayout (sidebar, top bar, content area from the mockup)
├── hooks/              reusable logic: usePagedList, ...
├── constants/          app.ts (time zone, currency, page sizes), routes.ts, one file per enum (productStatus.ts)
├── messages/           texts.ts: every text shown in the UI
├── theme/              theme.ts: the mockup's colours as Ant Design tokens, badge tones
├── styles/             global.css: the only global CSS
└── utils/              format.ts (dates, money), errors.ts (ApiError, messages, form errors), table.ts (paging/sorting)
```

The shared files above exist since T2.0; use and extend them instead of writing a second version in a page.

- Components and pages: one per file, `PascalCase` file and name (`ProductListPage.tsx`), function components only, named exports. Other files: `camelCase` (`client.ts`, `format.ts`).
- A page never calls Axios directly; it calls a function in `api/` that returns typed data.

### Types for API data

- Types mirror the backend DTO records with the **same names and fields**: `ProductResponse`, `ProductRequest`, `PageResponse<T>`, `ApiError`. When a DTO changes, its type changes in the same task.
- Backend enums are string union types in `types/`: `type ProductStatus = 'ACTIVE' | 'INACTIVE'`.
- Timestamps stay ISO-8601 strings (`"2026-10-22T08:15:30Z"`) in types and state; they are turned into text only for display.
- Money arrives as a JSON number. The frontend only **displays** and **sends** amounts; it never calculates totals with JavaScript numbers (binary floating point, like `double`). The backend does all money math with `BigDecimal` and returns the results.

### Calling the backend

- `api/client.ts` creates the one Axios instance: `baseURL` from `import.meta.env.VITE_API_BASE_URL`, empty by default, so calls go to `/api/...` on the same origin.
- **Development: Vite dev proxy instead of CORS.** `vite.config.ts` forwards `/api` to the backend: to product-service (`http://localhost:8082`) in Phase 2, to the api-gateway (`http://localhost:8080`) from Phase 3 on. The browser only sees `localhost:5173`, so no CORS is needed and the services never get CORS settings. CORS is configured once, in the gateway (T3.3).
- Lists: Ant Design tables count pages from 1, the API from 0, so send `page = current - 1`; send `size`, and `sort` as `field,asc|desc`. Filters use the backend parameter names (`search`, `categoryId`, `status`, ...).
- Date filters are sent as `YYYY-MM-DD` (`from`, `to`); the backend turns them into Vietnam-time ranges.
- Config values come from `VITE_*` environment variables, with `frontend/.env.example` committed and `.env` never committed.

### Errors

- `utils/errors.ts` turns every failed call into one `ApiError` (`status`, `code`, `detail`, `errors`), including network errors (no response at all).
- Decide by **`code`**, never by `detail` text:
  - `VALIDATION_FAILED`: put each `errors[]` entry on its form field (`form.setFields`), so the message appears under the field.
  - Other 4xx (`NOT_FOUND`, `DUPLICATE`, `IN_USE`, ...): show `detail` in an Ant Design message or notification. The backend already wrote it for people.
  - 5xx and network errors: a generic text from `messages/texts.ts`.
- From Phase 3: 401 → back to the login page, in the Axios response interceptor; 403 → a "no permission" text.

### Showing dates, money and enums (`utils/format.ts`, `constants/`)

- Dates: `formatDate(iso)` → `22/10/2026`, `formatDateTime(iso)` → `22/10/2026 01:30`, always in **Asia/Ho_Chi_Minh**, with the built-in `Intl.DateTimeFormat` (`timeZone: 'Asia/Ho_Chi_Minh'`). The same formats as the backend's `DateTimeUtils`. Never `toLocaleString()` without a time zone: it would use the viewer's computer settings.
- Money: `formatMoney(12.5)` → `$12.50` with `Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })`.
- Enums: never show raw codes. Each enum has one file in `constants/` with its label and badge color, e.g. `PRODUCT_STATUS = { ACTIVE: { label: 'Active', color: 'green' }, INACTIVE: { label: 'Inactive', color: 'default' } }`, used by a shared `StatusBadge`.

### UI texts (`messages/texts.ts`)

- Every text the UI shows (titles, buttons, column headers, confirmations, empty states, generic errors) lives in one typed object in `messages/texts.ts`, grouped like the backend: `common`, `category`, `supplier`, `product`, ... Pages use `texts.product.title`, never a string literal.
- Labels and wording follow the mockup (`docs/mockup/`).
- Error texts that come from the backend (`detail`, field messages) are shown as they are; the backend already looks them up in its messages files.
- No i18n library for now; the one-object structure makes adding one later a mechanical change.

### Routes

- Paths are constants in `constants/routes.ts` and follow the mockup: `/products`, `/products/new`, `/products/:id/edit`, `/categories`, `/suppliers`, ...
- Every page is inside `AppLayout`, except login (Phase 3).
