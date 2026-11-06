# inventory-service

Warehouses and stock for Stockline. Spring Boot 4.1, Java 21, port **8083**, database schema `inventory_db`. Built like [product-service](../product-service/README.md) (same layers, error format, tests and Docker setup); this README covers what is different.

Warehouses, stock levels and the movement history. The frontend pages come with T4.5.

## Run

Start the infrastructure first (from `infra/`): `docker compose up -d`. Install the shared library once: `cd libs/stockline-common` then `./mvnw install`.

```bash
./mvnw spring-boot:run      # Windows cmd/PowerShell: mvnw.cmd spring-boot:run
./mvnw test                 # needs Docker running (Testcontainers)
```

| URL | What |
|---|---|
| http://localhost:8083/actuator/health | Health check |
| http://localhost:8083/swagger-ui.html | Swagger UI. Log in on auth-service, click **Authorize**, paste the `accessToken` |

As a container (from `infra/`): `docker compose --profile services up -d --build inventory-service`.

## Who may call what

Like product-service: every request needs a valid JWT, except health and the API docs. Reading takes STAFF, changing (also adjusting stock) takes MANAGER; ADMIN through the role hierarchy. Rules: `config/WebSecurityConfig`.

## API

Errors use the JSON format from [docs/conventions.md](../../docs/conventions.md#3-json-error-format).

### Warehouses: `/api/warehouses`

| Method and path | Result | Errors |
|---|---|---|
| `GET /api/warehouses?search=&page=0&size=20&sort=code,asc` | `200`, a page of warehouses. `search` matches part of the code or name, ignoring case. Default sort `code,asc`, max size 100 | `400` unknown sort field |
| `GET /api/warehouses/{id}` | `200`, one warehouse | `404` |
| `POST /api/warehouses` | `201`, `Location: /api/warehouses/{id}` | `400` validation, `409 DUPLICATE` code |
| `PUT /api/warehouses/{id}` | `200`, the updated warehouse (every field is replaced) | `400`, `404`, `409 DUPLICATE` |
| `DELETE /api/warehouses/{id}` | `204` | `404`, `409 IN_USE` if it has stock levels |

Request body: `{ "code": "WH-01", "name": "Main Warehouse", "address": "District 7, HCMC" }`.

| Field | Rules |
|---|---|
| `code` | Required, max 50, letters, digits, `.`, `_`, `-` only; unique ignoring case |
| `name` | Required, max 100, trimmed |
| `address` | Optional, max 255; empty is stored as `null` |

Response: `{ "id", "code", "name", "address", "skuCount", "createdAt", "createdBy", "updatedAt", "updatedBy" }`. `skuCount` is the mockup's SKUs column: how many different products have a stock level here. A list gets all counts of a page with one grouped query.

### Stock levels: `/api/stock-levels`

| Method and path | Result | Errors |
|---|---|---|
| `GET /api/stock-levels?search=&warehouseId=&productId=&lowStock=&page=0&size=20&sort=productName,asc` | `200`, a page of stock levels. Filters are optional and combined with AND | `400` unknown sort field or filter value |
| `GET /api/stock-levels/{id}` | `200`, one stock level | `404` |
| `POST /api/stock-levels/adjust` | `200`, the changed stock level | See below |

| Filter | Matches |
|---|---|
| `search` | Part of the product name or SKU, ignoring case. `%` and `_` are plain text |
| `warehouseId`, `productId` | Exactly this one |
| `lowStock=true` | `available <= reorderLevel` |

Sort by `productName`, `productSku`, `onHand`, `reserved`, `reorderLevel` or `warehouse.code`. `available` isn't a column, so it can't be sorted.

Response: `{ "id", "warehouseId", "warehouseCode", "warehouseName", "productId", "productSku", "productName", "onHand", "reserved", "available", "reorderLevel", "lowStock", "createdAt", "createdBy", "updatedAt", "updatedBy" }`.

**Adjusting** (MANAGER): `{ "productId": 6, "warehouseId": 1, "quantity": -2, "reference": "Count" }`. `quantity` is the change, not the new total; `reference` is optional (max 100).

1. Reads the product from product-service, with the user's token. No transaction is open during this call.
2. In one transaction: changes `on_hand`, refreshes the copied SKU, name and reorder level, and writes an `ADJUST` movement. The first adjustment of a product in a warehouse creates its stock level.

| Situation | Status | `code` |
|---|---|---|
| Missing field, or `quantity` beyond ±1,000,000 | 400 | `VALIDATION_FAILED` |
| `quantity` is 0 | 400 | `BAD_REQUEST` |
| Unknown product or warehouse | 404 | `NOT_FOUND` |
| It would take more than `available`: reserved stock can't be adjusted away | 409 | `INSUFFICIENT_STOCK` |
| Someone changed the same row at the same moment (`@Version`) | 409 | `CONCURRENT_UPDATE`: reload and try again |
| product-service can't be reached | 503 | `SERVICE_UNAVAILABLE`; nothing is changed, and reading stock keeps working |

### Stock movements: `/api/stock-movements`

The history of stock, read-only: movements are written by the code that changes stock, in the same transaction, and never changed. `POST` gives 405.

| Method and path | Result | Errors |
|---|---|---|
| `GET /api/stock-movements?search=&warehouseId=&productId=&movementType=&from=&to=&page=0&size=20&sort=createdAt,desc` | `200`, a page of movements, newest first. Filters are optional and combined with AND | `400`, see below |

| Filter | Matches |
|---|---|
| `search` | Part of the product name or SKU, ignoring case. `%` and `_` are plain text |
| `warehouseId`, `productId` | Exactly this one |
| `movementType` | `IN`, `OUT`, `ADJUST`, `RESERVE` or `RELEASE` |
| `from`, `to` | Vietnam dates (`YYYY-MM-DD`), both included. `to=2026-10-31` covers all of 31 October in Vietnam, which ends at 16:59:59 UTC |

| Situation | Status | `code` |
|---|---|---|
| A date that isn't `YYYY-MM-DD`, or an unknown `movementType` | 400 | `VALIDATION_FAILED` |
| `from` after `to`, or an unknown sort field | 400 | `BAD_REQUEST` |

Response: `{ "id", "warehouseId", "warehouseCode", "productId", "productSku", "productName", "movementType", "quantity", "reference", "createdAt", "createdBy", "updatedAt", "updatedBy" }`. `quantity` is signed; `createdAt` and `createdBy` are the mockup's Time and By; the product name is as it was at that moment.

## Database

Schema `inventory_db`, created by Flyway. Every table has `id` and the four audit columns.

| Migration | Table | Columns | Rules |
|---|---|---|---|
| `V1__create_warehouses.sql` | `warehouses` | `code`, `name`, `address` (optional) | `code` unique |
| `V2__create_stock_levels.sql` | `stock_levels` | `warehouse_id`, `product_id`, `product_sku`, `product_name`, `reorder_level`, `on_hand`, `reserved`, `version` | One row per product and warehouse; `on_hand` â‰¥ 0; 0 â‰¤ `reserved` â‰¤ `on_hand` |
| `V3__create_stock_movements.sql` | `stock_movements` | `warehouse_id`, `product_id`, `product_sku`, `product_name`, `movement_type`, `quantity` (signed), `reference` | Never updated or deleted |
| `V4__create_stock_reservations.sql` | `stock_reservations` | `order_id`, `warehouse_id`, `product_id`, `quantity`, `status` | One per order line; `quantity` > 0 |
| `V5__create_processed_events.sql` | `processed_events` | `event_id`, `event_type` | `event_id` unique |

- `available` = `on_hand` âˆ’ `reserved`. It is calculated, never stored, so the numbers can't disagree.
- `product_id` and `order_id` have no foreign key: those rows live in other services' schemas. `warehouse_id` does, so a warehouse with stock can't be deleted.
- `product_sku`, `product_name` and `reorder_level` are copies from product-service. A query here can't join `product_db`, lists show them, the low-stock filter compares with `reorder_level` in SQL, and movements stay readable after a product is renamed or deleted. They are refreshed at each adjustment, so after a product changes, the old values show until the next adjustment; T6.9 makes this immediate with a `product.updated` event.
- `version` is for optimistic locking (`@Version`, T6.3): an `UPDATE` only succeeds if nobody changed the row since it was read.
- **CHECK constraints** are the last line of defence against negative stock and overselling. Code must check these rules first and throw its own error: Spring has no category for MySQL's CHECK error (3819), so it wouldn't become a clean 409 by itself.
- `movement_type`: `IN`, `OUT`, `ADJUST`, `RESERVE`, `RELEASE`. Reservation `status`: `RESERVED`, `CONFIRMED`, `RELEASED`. The enums come with the code that uses them.

## Tests

`./mvnw test`: 100 tests, Docker must be running.

| Class | What it checks |
|---|---|
| `service/StockAdjusterTest`, `service/StockLevelServiceTest` | Unit tests: new row, copies refreshed, movement written, reserved stock protected, zero and unknown refused |
| `service/StockMovementServiceTest` | Unit test: mapping, `from` after `to` refused |
| `client/ProductClientTest` | The call to product-service, with `MockRestServiceServer`: token sent, 404, no signed-in user |
| `controller/StockLevelControllerTest` | Web slice: filters, JSON, validation, `INSUFFICIENT_STOCK`, `CONCURRENT_UPDATE` |
| `controller/StockMovementControllerTest` | Web slice: every filter bound, newest first by default, bad date and type, 405 |
| `repository/StockMovementRepositoryTest` | MySQL: every filter, the Vietnam day boundary to the second, `created_at` stored in UTC |
| `repository/StockLevelRepositoryTest` | MySQL: every filter, low stock at the exact limit, sort by warehouse, `version` rising, movement columns |
| `StockIntegrationTest` | All layers on MySQL with product-service mocked: the adjustment lifecycle, refusals, and the resulting history |
| `service/WarehouseServiceTest` | Unit test: trimming, duplicates, SKU counts, delete rule |
| `controller/WarehouseControllerTest` | Web slice: URLs, JSON, validation, error format |
| `repository/WarehouseRepositoryTest` | MySQL: search, unique code ignoring case, SKU count |
| `WarehouseIntegrationTest` | All layers on MySQL: create, duplicate, update, in-use delete, delete |
| `constant/MessagesPropertiesTest` | messages.properties and `MessageKeys` in step |
| `repository/MigrationsTest` | The migrations on real MySQL: every table exists, defaults, unique keys, foreign keys, CHECK constraints, no foreign key on `product_id`. Plain SQL, for tables without an entity yet |
| `SecurityRulesTest` | Health and docs open, 401 without a token, what STAFF, MANAGER and ADMIN get through |
| `InventoryServiceApplicationTests` | The application starts |

## Configuration

| Variable | Default (local only) |
|---|---|
| `SERVER_PORT` | `8083` |
| `DB_URL` | `jdbc:mysql://localhost:3306/inventory_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `stockline` / `stockline` |
| `JWT_SECRET` | `local-dev-only-stockline-jwt-secret-change-me`; must equal auth-service's |
| `PRODUCT_SERVICE_URL` | `http://localhost:8082`; product-service directly, not through the gateway |

## New compared to Spring Boot 2.x / Java 8â€“11

| Old | Now | Notes |
|---|---|---|
| MySQL parsed `CHECK` and ignored it | MySQL 8.0.16+ enforces `CHECK` constraints | So the database itself refuses negative stock |
| `@DateTimeFormat` on a field with a setter | The same annotation on a `record` component | `LocalDate` query parameters; a wrong format is a normal validation error |
| `RestTemplate` | `RestClient` (Spring 6.1), from `spring-boot-starter-restclient` | A fluent API; `.onStatus(...)` turns a 404 into our own exception. Boot 4 made the HTTP clients separate starters |
| `MockRestServiceServer.createServer(restTemplate)` | `MockRestServiceServer.bindTo(builder)` | The same test tool for `RestClient` |
| `@Transactional` on everything | `Propagation.NOT_SUPPORTED` plus a separate transactional bean | So no database connection is held while waiting for another service. `@Transactional` works through a proxy, so it only applies to calls from another bean |
