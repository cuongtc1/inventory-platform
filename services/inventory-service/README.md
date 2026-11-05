# inventory-service

Warehouses and stock for Stockline. Spring Boot 4.1, Java 21, port **8083**, database schema `inventory_db`. Built like [product-service](../product-service/README.md) (same layers, error format, tests and Docker setup); this README covers what is different.

T4.1 is the scaffold: the service starts, Flyway creates the tables, and security is on. There are no endpoints yet; T4.2 to T4.4 add them.

## Run

Start the infrastructure first (from `infra/`): `docker compose up -d`. Install the shared library once: `cd libs/stockline-common` then `./mvnw install`.

```bash
./mvnw spring-boot:run      # Windows cmd/PowerShell: mvnw.cmd spring-boot:run
./mvnw test                 # needs Docker running (Testcontainers)
```

| URL | What |
|---|---|
| http://localhost:8083/actuator/health | Health check |
| http://localhost:8083/swagger-ui.html | Swagger UI (empty until T4.2) |

As a container (from `infra/`): `docker compose --profile services up -d --build inventory-service`.

## Who may call what

Like product-service: every request needs a valid JWT, except health and the API docs. Reading takes STAFF, changing takes MANAGER (ADMIN through the role hierarchy). Rules: `config/WebSecurityConfig`.

## Database

Schema `inventory_db`, created by Flyway. Every table has `id` and the four audit columns.

| Migration | Table | Columns | Rules |
|---|---|---|---|
| `V1__create_warehouses.sql` | `warehouses` | `code`, `name`, `address` (optional) | `code` unique |
| `V2__create_stock_levels.sql` | `stock_levels` | `warehouse_id`, `product_id`, `product_sku`, `product_name`, `reorder_level`, `on_hand`, `reserved`, `version` | One row per product and warehouse; `on_hand` ≥ 0; 0 ≤ `reserved` ≤ `on_hand` |
| `V3__create_stock_movements.sql` | `stock_movements` | `warehouse_id`, `product_id`, `product_sku`, `product_name`, `movement_type`, `quantity` (signed), `reference` | Never updated or deleted |
| `V4__create_stock_reservations.sql` | `stock_reservations` | `order_id`, `warehouse_id`, `product_id`, `quantity`, `status` | One per order line; `quantity` > 0 |
| `V5__create_processed_events.sql` | `processed_events` | `event_id`, `event_type` | `event_id` unique |

- `available` = `on_hand` − `reserved`. It is calculated, never stored, so the numbers can't disagree.
- `product_id` and `order_id` have no foreign key: those rows live in other services' schemas. `warehouse_id` does, so a warehouse with stock can't be deleted.
- `product_sku`, `product_name` and `reorder_level` are copies from product-service. A query here can't join `product_db`, lists show them, the low-stock filter compares with `reorder_level` in SQL, and movements stay readable after a product is renamed or deleted. How the copies are filled and kept fresh is decided in T4.3.
- `version` is for optimistic locking (`@Version`, T6.3): an `UPDATE` only succeeds if nobody changed the row since it was read.
- **CHECK constraints** are the last line of defence against negative stock and overselling. Code must check these rules first and throw its own error: Spring has no category for MySQL's CHECK error (3819), so it wouldn't become a clean 409 by itself.
- `movement_type`: `IN`, `OUT`, `ADJUST`, `RESERVE`, `RELEASE`. Reservation `status`: `RESERVED`, `CONFIRMED`, `RELEASED`. The enums come with the code that uses them.

## Tests

`./mvnw test`: 18 tests, Docker must be running.

| Class | What it checks |
|---|---|
| `repository/MigrationsTest` | The migrations on real MySQL: every table exists, defaults, unique keys, foreign keys, CHECK constraints, no foreign key on `product_id`. Plain SQL, because there are no entities yet |
| `SecurityRulesTest` | Health and docs open, 401 without a token, what STAFF, MANAGER and ADMIN get through |
| `InventoryServiceApplicationTests` | The application starts |

## Configuration

| Variable | Default (local only) |
|---|---|
| `SERVER_PORT` | `8083` |
| `DB_URL` | `jdbc:mysql://localhost:3306/inventory_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `stockline` / `stockline` |
| `JWT_SECRET` | `local-dev-only-stockline-jwt-secret-change-me`; must equal auth-service's |

## New compared to Spring Boot 2.x / Java 8–11

| Old | Now | Notes |
|---|---|---|
| MySQL parsed `CHECK` and ignored it | MySQL 8.0.16+ enforces `CHECK` constraints | So the database itself refuses negative stock |
