# product-service

Product catalog for Stockline: categories, suppliers and products. Spring Boot 4.1, Java 21, port **8082**, database schema `product_db`.

## Run

Start the infrastructure first (from `infra/`): `docker compose up -d`.

From this folder:

```bash
./mvnw spring-boot:run      # Windows cmd/PowerShell: mvnw.cmd spring-boot:run
./mvnw test                 # needs Docker running (Testcontainers)
./mvnw clean package        # builds target/product-service-0.0.1-SNAPSHOT.jar
```

`JAVA_HOME` must point to JDK 21. You don't need Maven installed: `mvnw` downloads the right Maven version the first time.

| URL | What |
|---|---|
| http://localhost:8082/actuator/health | Health check: `UP`, with details per component (`db`, `diskSpace`, ...) |
| http://localhost:8082/swagger-ui.html | Swagger UI: try every endpoint in the browser |
| http://localhost:8082/v3/api-docs | OpenAPI description (JSON) that Swagger UI reads |

## API

Try every endpoint in Swagger UI. Errors use the JSON format from [docs/conventions.md](../../docs/conventions.md#3-json-error-format).

### Errors

Every error, including Spring's own, has `type`, `title`, `status`, `detail`, `instance`, `code` and `timestamp` (`GlobalExceptionHandler`):

| Situation | Status | `code` |
|---|---|---|
| A field breaks a rule, or has the wrong type (`"status": "SOLD_OUT"`, `"categoryId": "abc"`, `?status=WRONG`) | 400 | `VALIDATION_FAILED`, with `errors: [{ field, message }]` |
| Broken or missing JSON, `/api/products/abc`, unknown sort field | 400 | `BAD_REQUEST` |
| Unknown id, unknown referenced id, unknown URL | 404 | `NOT_FOUND` |
| `PATCH` or another method an endpoint doesn't have | 405 | `METHOD_NOT_ALLOWED` |
| Body that isn't `application/json` | 415 | `UNSUPPORTED_MEDIA_TYPE` |
| Name or SKU already used (also when two requests arrive at once and only the database's unique key catches it) | 409 | `DUPLICATE` |
| Delete while still used (also when only the foreign key catches it) | 409 | `IN_USE` |
| Anything unexpected | 500 | `INTERNAL_ERROR`, with a generic `detail`; the stack trace is logged |

### Categories: `/api/categories`

| Method and path | Result | Errors |
|---|---|---|
| `GET /api/categories?search=&page=0&size=20&sort=name,asc` | `200`, a page of categories. `search` matches part of the name, ignoring case. Default sort `name,asc`, max size 100 | `400` unknown sort field |
| `GET /api/categories/{id}` | `200`, one category | `404` |
| `POST /api/categories` | `201`, `Location: /api/categories/{id}` | `400` validation, `404` parent not found, `409 DUPLICATE` name |
| `PUT /api/categories/{id}` | `200`, the updated category | same as POST, plus `400` if the parent is the category itself or one of its subcategories |
| `DELETE /api/categories/{id}` | `204` | `404`, `409 IN_USE` if it has subcategories or products |

Request body: `{ "name": "Cables", "parentId": 1 }`. `name` is required (max 100 characters, trimmed), and names are unique ignoring case. `parentId` is optional; leave it out for a top-level category.

Response: `{ "id", "name", "parentId", "parentName", "createdAt", "updatedAt" }`.

### Suppliers: `/api/suppliers`

| Method and path | Result | Errors |
|---|---|---|
| `GET /api/suppliers?search=&page=0&size=20&sort=name,asc` | `200`, a page of suppliers. `search` matches part of the name, ignoring case. Default sort `name,asc`, max size 100 | `400` unknown sort field |
| `GET /api/suppliers/{id}` | `200`, one supplier | `404` |
| `POST /api/suppliers` | `201`, `Location: /api/suppliers/{id}` | `400` validation, `409 DUPLICATE` name |
| `PUT /api/suppliers/{id}` | `200`, the updated supplier (every field is replaced; a missing field is cleared) | `400`, `404`, `409 DUPLICATE` |
| `DELETE /api/suppliers/{id}` | `204` | `404`, `409 IN_USE` if products still use it |

Request body: `{ "name": "LogiCo", "contactName": "Anna Pham", "email": "anna@logico.com", "phone": "+84 90 111 2233" }`.

| Field | Rules |
|---|---|
| `name` | Required, max 150, unique ignoring case |
| `contactName` | Optional, max 100 |
| `email` | Optional, max 150, must be a valid email address |
| `phone` | Optional, max 30, only digits, spaces, `+`, `-`, `(`, `)` |

Text is trimmed. An empty or blank optional field is stored as `null`.

Response: `{ "id", "name", "contactName", "email", "phone", "createdAt", "updatedAt" }`.

### Products: `/api/products`

| Method and path | Result | Errors |
|---|---|---|
| `GET /api/products?search=&categoryId=&supplierId=&status=&page=0&size=20&sort=name,asc` | `200`, a page of products. All filters are optional and combined with AND (see below). Default sort `name,asc`, max size 100 | `400` unknown sort field, `400 VALIDATION_FAILED` for a wrong filter value such as `status=WRONG` |
| `GET /api/products/{id}` | `200`, one product | `404` |
| `POST /api/products` | `201`, `Location: /api/products/{id}` | `400` validation, `404` category or supplier not found, `409 DUPLICATE` SKU |
| `PUT /api/products/{id}` | `200`, the updated product (every field is replaced) | same as POST, plus `404` product |
| `DELETE /api/products/{id}` | `204` | `404` |

List filters:

| Parameter | Matches |
|---|---|
| `search` | Part of the name **or** the SKU, ignoring case. `%` and `_` are plain text |
| `categoryId` | Products in exactly this category (not its subcategories) |
| `supplierId` | Products from this supplier |
| `status` | `ACTIVE` or `INACTIVE` |

Sort by any product field (`sku`, `name`, `salePrice`, `reorderLevel`, `createdAt`, ...) or by a related name: `sort=category.name,asc`, `sort=supplier.name,asc`.

Request body:

```json
{ "sku": "A01", "name": "Wireless Mouse", "categoryId": 3, "supplierId": 5,
  "costPrice": 8.00, "salePrice": 12.00, "reorderLevel": 20, "status": "ACTIVE" }
```

| Field | Rules |
|---|---|
| `sku` | Required, max 50, letters, digits, `.`, `_`, `-` only; unique ignoring case |
| `name` | Required, max 200, trimmed |
| `categoryId` | Required; the category must exist |
| `supplierId` | Optional; the supplier must exist when given |
| `costPrice` | Required, ≥ 0, at most 2 decimals |
| `salePrice` | Required, > 0, at most 2 decimals |
| `reorderLevel` | Required, ≥ 0 |
| `status` | Required, `ACTIVE` or `INACTIVE` |

Response: `{ "id", "sku", "name", "categoryId", "categoryName", "supplierId", "supplierName", "costPrice", "salePrice", "reorderLevel", "status", "createdAt", "updatedAt" }`. Prices always have 2 decimals.

Deleting a product is a hard delete. Deleting a category or supplier that products still use returns `409 IN_USE`.

## Configuration

All settings are in `src/main/resources/application.yml`. Values that differ per environment come from environment variables, with defaults for the local Docker setup:

| Variable | Default |
|---|---|
| `SERVER_PORT` | `8082` |
| `DB_URL` | `jdbc:mysql://localhost:3306/product_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `stockline` / `stockline` |

## Project files

| Path | Purpose |
|---|---|
| `pom.xml` | Maven build: Spring Boot parent, dependencies, plugins |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Maven wrapper: runs a fixed Maven version (3.10.0) without installing Maven |
| `src/main/java/com/stockline/product/ProductServiceApplication.java` | Main class. `@SpringBootApplication` turns on auto-configuration and scans `com.stockline.product` and its sub-packages for beans |
| `src/main/resources/application.yml` | Configuration |
| `src/main/resources/db/migration/` | Flyway SQL migrations, run in order at startup (see [Database](#database)) |
| `src/main/java/.../entity/` | JPA entities `Category`, `Supplier`, `Product`, the `ProductStatus` enum, and `BaseEntity` (id and timestamps) |
| `src/main/java/.../repository/` | Spring Data JPA repositories, one per entity |
| `src/main/java/.../controller/` | REST controllers (`CategoryController`, `SupplierController`, `ProductController`) |
| `src/main/java/.../service/` | Business rules and transactions (`CategoryService`, `SupplierService`, `ProductService`) |
| `src/main/java/.../repository/ProductSpecifications.java` | Builds the product list's WHERE clause from the filters that were sent |
| `src/main/java/.../dto/` | Request and response records, and `PageResponse` for lists |
| `src/main/java/.../mapper/` | Entity → response conversion |
| `src/main/java/.../exception/` | `ApiException` and its subclasses, and `GlobalExceptionHandler` (JSON errors) |
| `src/test/java/.../TestcontainersConfiguration.java` | Starts a throwaway MySQL 8.4 container for tests |
| `src/test/java/.../ProductServiceApplicationTests.java` | Starts the whole app against that container |
| `src/test/java/.../repository/*RepositoryTest.java` | `@DataJpaTest` on MySQL: mapping, unique keys, custom queries |
| `src/test/java/.../repository/ProductSpecificationsTest.java` | `@DataJpaTest`: every product filter, combinations, sort by category name |
| `src/test/java/.../service/*ServiceTest.java` | Mockito unit tests of the business rules |
| `src/test/java/.../controller/*ControllerTest.java` | `@WebMvcTest`: URLs, status codes, JSON and error format |
| `src/test/java/.../exception/GlobalExceptionHandlerTest.java` | `@WebMvcTest`: broken JSON, wrong types, 405, 415, unknown URL, 500, unique-key race |
| `src/test/java/.../exception/GlobalExceptionHandlerMySqlTest.java` | Real MySQL constraint errors (unique key, foreign key both ways) through the handler |

## Database

Schema `product_db`, created by Flyway:

| Migration | Table | Columns (besides `id`, `created_at`, `updated_at`) |
|---|---|---|
| `V1__create_categories.sql` | `categories` | `name` (unique), `parent_id` (optional, points to another category) |
| `V2__create_suppliers.sql` | `suppliers` | `name` (unique), `contact_name`, `email`, `phone` (all optional) |
| `V3__create_products.sql` | `products` | `sku` (unique), `name`, `category_id` (required), `supplier_id` (optional), `cost_price`, `sale_price`, `reorder_level`, `status` (`ACTIVE` / `INACTIVE`) |

Foreign keys: `categories.parent_id → categories`, `products.category_id → categories`, `products.supplier_id → suppliers`. Deleting a category or supplier that is still used fails. Stock quantities are not here; they belong to inventory-service.
| `.gitattributes`, `.gitignore` | Generated by Spring Initializr: keep `mvnw` with LF line endings, ignore `target/` |

## How the tests get a database

Tests never use the MySQL from `infra/`. `TestcontainersConfiguration` starts a new `mysql:8.4` container on a random port, and `@ServiceConnection` points the app's datasource at it, replacing the URL and password from `application.yml`. Flyway runs all migrations on the empty database, Hibernate validates the entities, the tests run, and the container is deleted. Docker Desktop must be running.

## New compared to Spring Boot 2.x / Java 8–11

| Old | Now | Notes |
|---|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` | Boot 4 split auto-configuration into smaller modules; each starter covers one technology |
| `spring-boot-starter-test` (one for everything) | `spring-boot-starter-<module>-test` (e.g. `...-webmvc-test`, `...-data-jpa-test`) | Each test starter brings JUnit 5, Mockito, AssertJ plus that module's test support |
| `flyway-core` was enough | `spring-boot-starter-flyway` + `flyway-mysql` | Flyway has its own starter; database support is a separate module since Flyway 10 |
| `javax.persistence`, `javax.validation` | `jakarta.persistence`, `jakarta.validation` | Java EE was renamed Jakarta EE (since Boot 3) |
| `@MockBean` | `@MockitoBean` | `@MockBean` was removed in Boot 4 |
| Test DB: H2 in-memory, or a shared dev DB | Testcontainers + `@ServiceConnection` | Tests run on the same MySQL as production, no manual URL setup |
| springdoc 1.x / 2.x | springdoc 3.x | Version line that matches Spring Boot 4 |
| Mockito attached itself silently | `-javaagent` in `maven-surefire-plugin` | JDK 21 warns about agents loaded at runtime (JEP 451); a later JDK will block it |
| Jackson 2 (`com.fasterxml.jackson`) | Jackson 3 (`tools.jackson`) for JSON | Annotations like `@JsonProperty` keep their old package |
| `java.util.Date` / `LocalDateTime` in entities | `java.time.Instant` | Hibernate 6+ maps `Instant` directly to a UTC timestamp; with `hibernate.jdbc.time_zone: UTC` it is stored as UTC in `DATETIME(6)` |
| `org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` | Moved with the Boot 4 module split; `TestEntityManager` is now in `org.springframework.boot.jpa.test.autoconfigure` |
| `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` | Same module split |
| `org.springframework.data.mapping.PropertyReferenceException` | `org.springframework.data.core.PropertyReferenceException` | Moved in Spring Data 4 |
| DTO classes with getters, `equals`, `hashCode` | Java `record` (Java 16) | One line per field; immutable; Jackson and Bean Validation support them |
| Custom error JSON classes | `ProblemDetail` (Spring 6, RFC 9457) | Spring 7 leaves out `"type"` while it is `about:blank`, so the handler sets it explicitly |
| String concatenation for JSON in tests | Text blocks `"""..."""` (Java 15) | Multi-line strings without escaping quotes |
| `list.get(0)` | `list.getFirst()` (Java 21) | Sequenced collections |
| `Specification.where(null).and(...)` | `Specification.allOf(list)`, `Specification.unrestricted()` | Spring Data JPA 4; `JpaSpecificationExecutor` also gained `delete(PredicateSpecification)`, so `delete(any())` in Mockito is now ambiguous: use `any(Product.class)` |
| `com.fasterxml.jackson.databind.exc.InvalidFormatException` (checked `IOException` subclass) | `tools.jackson.databind.exc.InvalidFormatException` (unchecked, `RuntimeException`) | Jackson 3 exceptions moved package and no longer need `throws` |
| `if (x instanceof Foo) { Foo f = (Foo) x; ... }` | `if (x instanceof Foo f) { ... }` (Java 16) | Pattern matching for `instanceof`, used in the error handler |
| `switch` statement with `break` | `switch` expression with `->` returning a value (Java 14) | Used to map the constraint kind to a response |
| Query parameters bound to a JavaBean with setters | Bound to a `record` through its constructor | `ProductFilter` needs no annotation; `@ParameterObject` shows its fields in Swagger |
