# order-service

Customers and sales orders for Stockline. Spring Boot 4.1, Java 21, port **8084**, database schema `order_db`. Built like [product-service](../product-service/README.md) (same layers, error format, tests and Docker setup); this README covers what is different.

Customers are done. Sales orders come with T5.3 and T5.4, the frontend pages with T5.5.

## Run

Start the infrastructure first (from `infra/`): `docker compose up -d`. Install the shared library once: `cd libs/stockline-common` then `./mvnw install`.

```bash
./mvnw spring-boot:run      # Windows cmd/PowerShell: mvnw.cmd spring-boot:run
./mvnw test                 # needs Docker running (Testcontainers)
```

| URL | What |
|---|---|
| http://localhost:8084/actuator/health | Health check |
| http://localhost:8084/swagger-ui.html | Swagger UI. Log in on auth-service, click **Authorize**, paste the `accessToken` |

As a container (from `infra/`): `docker compose --profile services up -d --build order-service`.

## Who may call what

Like the other services: every request needs a valid JWT, except health and the API docs. Rules: `config/WebSecurityConfig`.

| | STAFF | MANAGER, ADMIN |
|---|---|---|
| Read customers | Yes | Yes |
| Add and edit customers | Yes: staff take the orders | Yes |
| Delete customers | No | Yes |
| Anything else that changes data | No, until a task says otherwise | Yes |

## API

Errors use the JSON format from [docs/conventions.md](../../docs/conventions.md#3-json-error-format).

### Customers: `/api/customers`

| Method and path | Result | Errors |
|---|---|---|
| `GET /api/customers?search=&page=0&size=20&sort=name,asc` | `200`, a page of customers. `search` matches part of the name or email, ignoring case. Default sort `name,asc`, max size 100 | `400` unknown sort field |
| `GET /api/customers/{id}` | `200`, one customer | `404` |
| `POST /api/customers` | `201`, `Location: /api/customers/{id}` | `400` validation, `409 DUPLICATE` name |
| `PUT /api/customers/{id}` | `200`, the updated customer (every field is replaced) | `400`, `404`, `409 DUPLICATE` |
| `DELETE /api/customers/{id}` | `204` | `403` for STAFF, `404`, `409 IN_USE` if it has orders |

Request body: `{ "name": "ACME Ltd", "email": "buy@acme.com", "phone": "+84 28 3800 1122" }`.

| Field | Rules |
|---|---|
| `name` | Required, max 150, trimmed; unique ignoring case |
| `email` | Optional, max 150, a valid address; empty is stored as `null` |
| `phone` | Optional, max 30, digits, spaces, `+`, `-`, `(` and `)`; empty is stored as `null` |

Response: `{ "id", "name", "email", "phone", "orderCount", "createdAt", "createdBy", "updatedAt", "updatedBy" }`. `orderCount` is the mockup's Orders column. A list gets all counts of a page with one query. Sorting by it isn't offered: it isn't a column.

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

`./mvnw test`: 53 tests, Docker must be running.

| Class | What it checks |
|---|---|
| `service/CustomerServiceTest` | Unit test: trimming, blank contacts become `null`, duplicates, order counts, delete refused with orders |
| `controller/CustomerControllerTest` | Web slice: URLs, status codes, JSON, validation, `DUPLICATE`, `IN_USE` |
| `repository/CustomerRepositoryTest` | MySQL: search, a customer without email isn't lost, name unique ignoring case, order count query |
| `repository/MigrationsTest` | The migrations on real MySQL: every table exists, defaults, unique keys, foreign keys, CHECK constraints, no foreign key leaving the schema. Plain SQL, for the tables without entities yet |
| `constant/MessagesPropertiesTest` | `messages.properties` and the code stay in step |
| `SecurityRulesTest` | Health and docs open, 401 without a token, what STAFF, MANAGER and ADMIN get through |
| `CustomerIntegrationTest` | All layers on MySQL: the customer lifecycle, including delete refused with an order |
| `OrderServiceApplicationTests` | The application starts |

## Configuration

| Variable | Default (local only) |
|---|---|
| `SERVER_PORT` | `8084` |
| `DB_URL` | `jdbc:mysql://localhost:3306/order_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `stockline` / `stockline` |
| `JWT_SECRET` | `local-dev-only-stockline-jwt-secret-change-me`; must equal auth-service's |
