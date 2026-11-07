# order-service

Customers and sales orders for Stockline. Spring Boot 4.1, Java 21, port **8084**, database schema `order_db`. Built like [product-service](../product-service/README.md) (same layers, error format, tests and Docker setup); this README covers what is different.

T5.1 is the scaffold: the service starts, Flyway creates the tables, and security is on. There are no endpoints yet; T5.2 to T5.4 add them.

## Run

Start the infrastructure first (from `infra/`): `docker compose up -d`. Install the shared library once: `cd libs/stockline-common` then `./mvnw install`.

```bash
./mvnw spring-boot:run      # Windows cmd/PowerShell: mvnw.cmd spring-boot:run
./mvnw test                 # needs Docker running (Testcontainers)
```

| URL | What |
|---|---|
| http://localhost:8084/actuator/health | Health check |
| http://localhost:8084/swagger-ui.html | Swagger UI (empty until T5.2) |

As a container (from `infra/`): `docker compose --profile services up -d --build order-service`.

## Who may call what

Like the other services: every request needs a valid JWT, except health and the API docs. For now reading takes STAFF and changing takes MANAGER (ADMIN through the role hierarchy). T5.2 and T5.3 refine this. Rules: `config/WebSecurityConfig`.

## Database

Schema `order_db`, created by Flyway. Every table has `id` and the four audit columns.

| Migration | Table | Columns | Rules |
|---|---|---|---|
| `V1__create_customers.sql` | `customers` | `name`, `email` and `phone` (optional) | `name` unique |
| `V2__create_sales_orders.sql` | `sales_orders` | `order_number`, `customer_id`, `warehouse_id`, `warehouse_code`, `status`, `total_amount`, `version` | `order_number` unique; `total_amount` ≥ 0; a customer with orders can't be deleted |
| `V3__create_sales_order_items.sql` | `sales_order_items` | `sales_order_id`, `product_id`, `product_sku`, `product_name`, `quantity`, `unit_price` | One line per product on an order; `quantity` > 0; `unit_price` ≥ 0 |
| `V4__create_order_status_history.sql` | `order_status_history` | `sales_order_id`, `from_status`, `to_status`, `note` | One row per status change; `from_status` is empty on the first; never updated or deleted |

- **Snapshots:** `product_sku`, `product_name` and `unit_price` are copied when the order is created, so renaming a product or changing its price never rewrites an order already placed.
- **No foreign key across schemas:** `product_id` and `warehouse_id` are plain columns, because those rows live in other services. `warehouse_code` is a copy of what lists show.
- **Line totals** are `quantity × unit_price`: calculated, never stored. `total_amount` on the order is stored, because the list shows and sorts by it, and an order's lines never change after it is created.
- **Who and when:** an order's creator, and the author of each status change, are the audit columns. A change made by a Kafka event has `system` there.
- **`version`** is for optimistic locking (`@Version`): a user and a Kafka event changing the status at once can't overwrite each other.
- **`status`:** `PENDING`, `RESERVED`, `PAID`, `SHIPPED`, `COMPLETED`, `REJECTED`, `CANCELLED`. The enum comes with the code that uses it (T5.4).
- **Phase 6** adds `outbox_events` and `processed_events` for the saga, with the tasks that use them.

## Tests

`./mvnw test`: 21 tests, Docker must be running.

| Class | What it checks |
|---|---|
| `repository/MigrationsTest` | The migrations on real MySQL: every table exists, defaults, unique keys, foreign keys, CHECK constraints, no foreign key leaving the schema. Plain SQL, because there are no entities yet |
| `SecurityRulesTest` | Health and docs open, 401 without a token, what STAFF, MANAGER and ADMIN get through |
| `OrderServiceApplicationTests` | The application starts |

## Configuration

| Variable | Default (local only) |
|---|---|
| `SERVER_PORT` | `8084` |
| `DB_URL` | `jdbc:mysql://localhost:3306/order_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `stockline` / `stockline` |
| `JWT_SECRET` | `local-dev-only-stockline-jwt-secret-change-me`; must equal auth-service's |
