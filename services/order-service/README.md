# order-service

Customers and sales orders for Stockline. Spring Boot 4.1, Java 21, port **8084**, database schema `order_db`. Built like [product-service](../product-service/README.md) (same layers, error format, tests and Docker setup); this README covers what is different.

Customers, and sales orders with their state machine, are done. The frontend pages come with T5.5, the Kafka saga and the stock effects of pay, ship and cancel with Phase 6.

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
| Read customers and sales orders | Yes | Yes |
| Add and edit customers | Yes: staff take the orders | Yes |
| Create sales orders | Yes | Yes |
| Mark orders paid, shipped, completed | Yes | Yes |
| Cancel orders | No | Yes |
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

### Sales orders: `/api/sales-orders`

| Method and path | Result | Errors |
|---|---|---|
| `GET /api/sales-orders?search=&customerId=&status=&page=0&size=20&sort=createdAt,desc` | `200`, a page of orders without their lines, newest first. `search` matches part of the order number or the customer's name | `400` unknown sort field or status |
| `GET /api/sales-orders/{id}` | `200`, one order with its lines | `404` |
| `POST /api/sales-orders` | `201`, `Location: /api/sales-orders/{id}`, the new order as `PENDING` | See below |

Request body: only ids and quantities. SKU, name and price come from product-service and the warehouse code from inventory-service, so a client can't set its own price.

```json
{ "customerId": 1, "warehouseId": 2, "items": [ { "productId": 6, "quantity": 5 }, { "productId": 7, "quantity": 2 } ] }
```

| Field | Rules |
|---|---|
| `customerId`, `warehouseId` | Required, must exist |
| `items` | 1 to 50 lines; each product at most once |
| `items[].productId` | Required, must exist and be `ACTIVE` |
| `items[].quantity` | 1 to 1,000,000 |

| Situation | Status | `code` |
|---|---|---|
| A missing or wrong field; the error names the line, e.g. `items[1].quantity` | 400 | `VALIDATION_FAILED` |
| A product twice, an inactive product, or a total above $9,999,999,999.99 | 400 | `BAD_REQUEST` |
| Unknown customer, warehouse or product | 404 | `NOT_FOUND` |
| product-service or inventory-service can't be reached | 503 | `SERVICE_UNAVAILABLE` |

Order response: `{ "id", "orderNumber", "customerId", "customerName", "warehouseId", "warehouseCode", "status", "totalAmount", "items": [ { "id", "productId", "productSku", "productName", "quantity", "unitPrice", "lineTotal" } ], "createdAt", "createdBy", "updatedAt", "updatedBy" }`. A list row has no `items`.

How creating works:

1. Cheap checks first, so a bad request costs no calls: each product only once, and the customer exists.
2. The warehouse and every product are read from their services, with the user's token (`client/ForwardUserToken`), outside any transaction: a slow service mustn't hold a database connection.
3. `SalesOrderCreator` saves everything in one transaction: the order number, the order, its lines and the first history row (`→ PENDING`). Either all of it is saved or none.

### Order status: the state machine

```
PENDING ──► RESERVED ──► PAID ──► SHIPPED ──► COMPLETED
   │            │          │
   ├──► REJECTED │          │
   └────────────┴──────────┴──► CANCELLED      (only before shipping)
```

| Method and path | Change | Who |
|---|---|---|
| `POST /api/sales-orders/{id}/pay` | RESERVED → PAID (a fake payment) | STAFF and up |
| `POST /api/sales-orders/{id}/ship` | PAID → SHIPPED | STAFF and up |
| `POST /api/sales-orders/{id}/complete` | SHIPPED → COMPLETED, once the customer has the goods | STAFF and up |
| `POST /api/sales-orders/{id}/cancel` | PENDING, RESERVED or PAID → CANCELLED | MANAGER and up |
| `GET /api/sales-orders/{id}/history` | The timeline, oldest first: `[ { "id", "fromStatus", "toStatus", "note", "createdAt", "createdBy" } ]` | STAFF and up |

No body; each action answers `200` with the whole order. Errors: `404` unknown order, `409 INVALID_STATUS_TRANSITION` when the current status doesn't allow it ("Order SO-1001 is PENDING, so it can not change to PAID"; nothing is saved), `409 CONCURRENT_UPDATE` when someone else changed the order at the same moment (`@Version`).

- **One place decides:** `OrderStatus.nextStatuses()` holds the table above. `SalesOrder.changeStatus` asks it and refuses, and `OrderStatusService.changeStatus` is the only caller, so every change is checked and gets a history row in the same transaction.
- **RESERVED and REJECTED** are set by the saga in Phase 6 (T6.2), through the same `OrderStatusService.changeStatus`, with `system` as the author. Until then a real order stays PENDING: it can be cancelled, but not paid.
- **Stock:** these actions only change the status for now. T6.6 adds what they do to stock: shipping confirms the reservation and lowers `on_hand`, cancelling releases it.

**Order numbers** come from the `number_sequences` table, starting at `SO-1001`. Taking a number locks its row (`SELECT ... FOR UPDATE`) until the order is saved, so two orders at the same moment can't get the same number, and a refused order gives its number back (no gaps).

## Database

Schema `order_db`, created by Flyway. Every table has `id` and the four audit columns.

| Migration | Table | Columns | Rules |
|---|---|---|---|
| `V1__create_customers.sql` | `customers` | `name`, `email` and `phone` (optional) | `name` unique |
| `V2__create_sales_orders.sql` | `sales_orders` | `order_number`, `customer_id`, `warehouse_id`, `warehouse_code`, `status`, `total_amount`, `version` | `order_number` unique; `total_amount` ≥ 0; a customer with orders can't be deleted |
| `V3__create_sales_order_items.sql` | `sales_order_items` | `sales_order_id`, `product_id`, `product_sku`, `product_name`, `quantity`, `unit_price` | One line per product on an order; `quantity` > 0; `unit_price` ≥ 0 |
| `V4__create_order_status_history.sql` | `order_status_history` | `sales_order_id`, `from_status`, `to_status`, `note` | One row per status change; `from_status` is empty on the first; never updated or deleted |
| `V5__create_number_sequences.sql` | `number_sequences` | `name`, `next_value` | One row per counter, `name` unique; `sales_order` starts at 1001 |

- **Snapshots:** `product_sku`, `product_name` and `unit_price` are copied when the order is created, so renaming a product or changing its price never rewrites an order already placed.
- **No foreign key across schemas:** `product_id` and `warehouse_id` are plain columns, because those rows live in other services. `warehouse_code` is a copy of what lists show.
- **Line totals** are `quantity × unit_price`: calculated, never stored. `total_amount` on the order is stored, because the list shows and sorts by it, and an order's lines never change after it is created.
- **Who and when:** an order's creator, and the author of each status change, are the audit columns. A change made by a Kafka event has `system` there.
- **`version`** is for optimistic locking (`@Version`): a user and a Kafka event changing the status at once can't overwrite each other.
- **`status`:** `PENDING`, `RESERVED`, `PAID`, `SHIPPED`, `COMPLETED`, `REJECTED`, `CANCELLED` (`constant/OrderStatus`). Orders start as `PENDING`; there are no drafts. Which change is allowed comes with T5.4.
- **Phase 6** adds `outbox_events` and `processed_events` for the saga, with the tasks that use them.

## Tests

`./mvnw test`: 113 tests, Docker must be running.

| Class | What it checks |
|---|---|
| `constant/OrderStatusTest` | The state machine as a table: all 49 pairs of statuses, cancel only before shipping, the final statuses |
| `entity/SalesOrderTest` | `changeStatus` returns the old status, refuses without changing anything, the happy path ends COMPLETED |
| `service/OrderStatusServiceTest` | Unit test: a history row with from, to and note; nothing recorded when refused; 404 |
| `service/CustomerServiceTest` | Unit test: trimming, blank contacts become `null`, duplicates, order counts, delete refused with orders |
| `service/SalesOrderServiceTest` | Unit test: what reaches the creator; a product twice, an unknown customer, an inactive product and a too-large total are refused before saving |
| `service/OrderNumberGeneratorTest` | MySQL, 8 threads at once: 40 numbers, no duplicates, none skipped; refuses to run outside a transaction. Fails with duplicates if the row lock is removed |
| `client/ClientsTest` | Both REST clients with `MockRestServiceServer`: token forwarded, fields read, 404, no signed-in user |
| `controller/CustomerControllerTest` | Web slice: URLs, status codes, JSON, validation, `DUPLICATE`, `IN_USE` |
| `controller/SalesOrderControllerTest` | Web slice: 201 and JSON, errors naming the line (`items[1].quantity`), filters, newest first, each action asks for its status, 409 text, history JSON |
| `repository/CustomerRepositoryTest` | MySQL: search, a customer without email isn't lost, name unique ignoring case |
| `repository/SalesOrderRepositoryTest` | MySQL: lines saved with the order, total, filters, `%` as plain text, history order, order count per customer |
| `repository/SalesOrderVersionTest` | MySQL: a change based on an old reading is refused (`@Version`); fails if `@Version` is removed |
| `repository/MigrationsTest` | The migrations on real MySQL: every table exists, defaults, unique keys, foreign keys, CHECK constraints, no foreign key leaving the schema |
| `constant/MessagesPropertiesTest` | `messages.properties` and the code stay in step |
| `SecurityRulesTest` | Health and docs open, 401 without a token, what STAFF, MANAGER and ADMIN get through |
| `CustomerIntegrationTest` | All layers on MySQL: the customer lifecycle, including delete refused with an order |
| `OrderLifecycleIntegrationTest` | All layers on MySQL: PENDING to COMPLETED with five history rows, wrong order 409 with nothing recorded, cancel once and only before shipping, 404 |
| `SalesOrderIntegrationTest` | All layers on MySQL with the other services mocked: copies kept after a product change, first history row, consecutive numbers, a refused order saves nothing and uses no number |
| `OrderServiceApplicationTests` | The application starts |

## Configuration

| Variable | Default (local only) |
|---|---|
| `SERVER_PORT` | `8084` |
| `DB_URL` | `jdbc:mysql://localhost:3306/order_db` |
| `DB_USERNAME` / `DB_PASSWORD` | `stockline` / `stockline` |
| `JWT_SECRET` | `local-dev-only-stockline-jwt-secret-change-me`; must equal auth-service's |
| `PRODUCT_SERVICE_URL` | `http://localhost:8082` |
| `INVENTORY_SERVICE_URL` | `http://localhost:8083` |

## New compared to Spring Boot 2.x / Java 8–11

| Old | Now | Notes |
|---|---|---|
| `@Lock` known from JPA, rarely needed | `@Lock(LockModeType.PESSIMISTIC_WRITE)` on a Spring Data query method | `SELECT ... FOR UPDATE` for the order number counter |
| `@Transactional` with the default propagation | `Propagation.MANDATORY` | The number generator refuses to run without the caller's transaction |
| `List<Item>` checked with a custom validator | `List<@Valid @NotNull Item>` | Type-use annotations (Bean Validation 2.0); errors name the line |
| `ExecutorService` closed with `shutdown()` in `finally` | `try (ExecutorService pool = ...)` | Java 19+: `ExecutorService` is `AutoCloseable` and waits for its tasks |
| `switch` statement with `case X: ... break;` | `switch` expression: `case PENDING -> Set.of(...)` | Returns a value, no fall-through; over an enum it must cover every value or it doesn't compile (`OrderStatus.nextStatuses`) |
| One test method per value | `@ParameterizedTest @EnumSource(OrderStatus.class)` | JUnit 5 runs the test once for each status |
| Interceptors on `RestTemplate` | `RestClient.Builder.requestInterceptor(...)` | `ForwardUserToken` adds the user's token to every call |
