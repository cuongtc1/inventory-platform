# Stockline: Inventory & Order Platform (TASKS.md)

A learning project: a microservices inventory and order system. Claude acts as tech lead and does most of the work. The developer reviews each task before the next one starts.

## 1. Working agreement

- **Owner:** `Claude` (Claude Code does it) or `You` (you do it or run/verify it). Change the owner of any task whenever you want.
- **Status:** `TODO` → `IN PROGRESS` → `REVIEW` → `DONE`. Claude moves tasks to `REVIEW`; only you mark them `DONE`.
- **Flow per task:** Claude works on a branch `feature/<task-id>-short-name`, runs the tests, commits, and writes a short summary (what changed, how to run it, what to check). You review, then merge to `main`.
- **Rules for Claude:** one task at a time, never touch other services' code, no secrets in Git, ask when a requirement is unclear.
- **Definition of Done (every task):** it builds, tests pass, acceptance criteria met, README or docs updated if behavior changed, committed with a clear message.

## 2. Architecture decisions

| Topic | Decision |
|---|---|
| Repo | One monorepo: `inventory-platform` |
| Backend | Java 21 (JDK 21.0.12.1), Spring Boot (latest stable that supports Java 21), Maven wrapper, Hibernate/JPA |
| Database | MySQL 8 in Docker, one schema per service, **no foreign keys across schemas**, Flyway migrations |
| Messaging | Kafka (KRaft mode, no Zookeeper) in Docker |
| Frontend | React + Vite + Ant Design |
| Auth | Plain JWT (no refresh token). One `user_type` column on `users`: STAFF(1), MANAGER(2), ADMIN(3). A higher level includes the lower ones. Stored as a string in MySQL. |
| Payment | Fake "Mark as paid" button |
| Tests | JUnit 5 + Mockito (unit), Spring tests for controllers and repositories |

Services and ports: `api-gateway` 8080, `auth-service` 8081, `product-service` 8082, `inventory-service` 8083, `order-service` 8084, `notification-service` 8085, MySQL 3306, Kafka 9092, frontend 5173.

Kafka topics: `order.created`, `stock.reserved`, `stock.failed`, `order.cancelled`, `stock.low`.

Order status flow: `PENDING → RESERVED → PAID → SHIPPED → COMPLETED`, plus `REJECTED` (stock failed) and `CANCELLED` (allowed before shipping).

Target repo layout:
```
inventory-platform/
├── services/   api-gateway, auth-service, product-service,
│               inventory-service, order-service, notification-service
├── frontend/
├── infra/      docker-compose.yml, mysql-init/
├── docs/       mockup/ (Stockline HTML mockup), conventions.md
├── TASKS.md
└── README.md
```

## 3. Phase 0: Setup (about 1 day)

| ID | Task | Acceptance criteria | Owner | Status |
|---|---|---|---|---|
| T0.1 | Repo skeleton | Folders above exist, `.gitignore` (Java, Node, IDE, env files), README with project overview, mockup unzipped into `docs/mockup/` | Claude | DONE |
| T0.2 | Check toolchain | `java -version`, `git --version`, `docker --version`, `node -v` OK; Claude confirms Spring Boot supports your JDK | You | DONE |
| T0.3 | Infra with Docker Compose | `docker compose up -d` in `infra/` starts MySQL (schemas `auth_db`, `product_db`, `inventory_db`, `order_db`, `notification_db` created by init script) and Kafka; Kafka UI optional. Connect from MySQL Workbench works | Claude | DONE |
| T0.4 | Conventions | `docs/conventions.md`: package naming, REST URL style, JSON error format, pagination format, branch and commit style | Claude | DONE |

## 4. Phase 1: product-service (about 3 days)

Goal: learn the standard layered Spring Boot service (controller / service / repository / entity / dto).

| ID | Task | Acceptance criteria | Owner | Status |
|---|---|---|---|---|
| T1.1 | Scaffold service | Spring Boot project in `services/product-service` (Web, Data JPA, MySQL, Validation, Actuator, Flyway, OpenAPI/Swagger UI); connects to `product_db`; `/actuator/health` is UP | Claude | DONE |
| T1.2 | Database migrations and entities | Flyway scripts for `categories`, `suppliers`, `products` (SKU unique); JPA entities match | Claude | TODO |
| T1.3 | Category CRUD API | Create, read, update, delete; optional parent category; validation | Claude | TODO |
| T1.4 | Supplier CRUD API | Same pattern as T1.3 | Claude | TODO |
| T1.5 | Product CRUD API | CRUD plus list with pagination, sort, search by name/SKU, filter by category, supplier and status | Claude | TODO |
| T1.6 | Error handling | Global exception handler; consistent JSON errors (400 validation, 404, 409 duplicate SKU) | Claude | TODO |
| T1.7 | Unit tests | Service tests with Mockito; controller tests; at least one repository test; `mvn test` passes | Claude | TODO |
| T1.8 | Dockerize | Dockerfile (multi-stage); service added to compose and runs in a container | Claude | TODO |
| T1.9 | Review | Try all endpoints in Swagger UI; read the code and ask questions | You | TODO |

## 5. Phase 2: React foundation (about 3 days)

| ID | Task | Acceptance criteria | Owner | Status |
|---|---|---|---|---|
| T2.1 | Scaffold frontend | Vite + React + Ant Design + React Router + Axios; runs on 5173; short README explaining React basics used (components, state, effects) | Claude | TODO |
| T2.2 | App layout | Dark sidebar with collapse button, white content area, top bar with avatar and **Log out** button, matching `docs/mockup/` | Claude | TODO |
| T2.3 | Product pages | List (search, filters, pagination, Edit/Delete icon buttons) and form, calling product-service | Claude | TODO |
| T2.4 | Category and Supplier pages | Same pattern as T2.3 | Claude | TODO |
| T2.5 | Review | You click through every page; list UI issues | You | TODO |

## 6. Phase 3: auth-service and api-gateway (about 3 days)

| ID | Task | Acceptance criteria | Owner | Status |
|---|---|---|---|---|
| T3.1 | auth-service | `users` table with `user_type`; BCrypt passwords; `POST /auth/login` returns JWT (contains user id and role); seeded admin account | Claude | TODO |
| T3.2 | User management API | ADMIN only: list, create, update, deactivate users; `GET /auth/me`; change password | Claude | TODO |
| T3.3 | api-gateway | Spring Cloud Gateway routes `/api/auth/**`, `/api/products/**` and so on; validates JWT; CORS for the frontend | Claude | TODO |
| T3.4 | Role rules | Hierarchy check (ADMIN ≥ MANAGER ≥ STAFF) enforced in the backend, e.g. only MANAGER+ can edit products, only ADMIN manages users | Claude | TODO |
| T3.5 | Frontend login | Login page, token storage, route guards, role-based menu (like the mockup's "View as"), Users page (ADMIN), Log out | Claude | TODO |
| T3.6 | Tests and review | Unit tests for JWT and role logic; you test all three roles | Claude + You | TODO |

## 7. Phase 4: inventory-service (about 3 days)

| ID | Task | Acceptance criteria | Owner | Status |
|---|---|---|---|---|
| T4.1 | Scaffold and migrations | Service, Flyway for `warehouses`, `stock_levels` (with `version`), `stock_movements`, `stock_reservations`, `processed_events` | Claude | TODO |
| T4.2 | Warehouse CRUD | API plus tests | Claude | TODO |
| T4.3 | Stock levels and adjustment | List stock (filters, low-stock only); adjust stock writes a movement record; `available = on_hand − reserved` | Claude | TODO |
| T4.4 | Movements API | Filter by product, warehouse, type, date | Claude | TODO |
| T4.5 | Frontend pages | Warehouses, Stock Levels, Stock Movements, Low-Stock Alerts | Claude | TODO |
| T4.6 | Tests and review | Unit tests; you review | Claude + You | TODO |

## 8. Phase 5: order-service basics (about 3 days)

| ID | Task | Acceptance criteria | Owner | Status |
|---|---|---|---|---|
| T5.1 | Scaffold and migrations | `customers`, `sales_orders`, `sales_order_items` (with product name snapshot), `order_status_history` | Claude | TODO |
| T5.2 | Customer CRUD | API plus tests | Claude | TODO |
| T5.3 | Sales order creation | Create draft/PENDING order with items; fetches product name and price from product-service (REST); order number `SO-xxxx` | Claude | TODO |
| T5.4 | State machine | Allowed transitions enforced in one place; invalid transition returns 409; every change recorded in history | Claude | TODO |
| T5.5 | Frontend pages | Customers, Sales Order list, New Sales Order, Sales Order Detail with stepper and timeline | Claude | TODO |

## 9. Phase 6: Kafka and the stock-reservation saga (about 4 days, the hardest part)

| ID | Task | Acceptance criteria | Owner | Status |
|---|---|---|---|---|
| T6.1 | Kafka basics | Topics created; Spring Kafka configured in order-service and inventory-service; simple producer/consumer test; Claude explains topics, partitions, consumer groups | Claude | TODO |
| T6.2 | Place order flow | "Place order" publishes `order.created`; inventory-service reserves stock and publishes `stock.reserved` or `stock.failed`; order moves to RESERVED or REJECTED | Claude | TODO |
| T6.3 | Concurrency safety | Optimistic locking with retry on `stock_levels`; test with two orders competing for the last items; no overselling | Claude | TODO |
| T6.4 | Idempotency | `processed_events` stops duplicate messages from being applied twice; test by replaying an event | Claude | TODO |
| T6.5 | Outbox pattern | Order is saved and the event stored in `outbox_events` in one transaction; a scheduled publisher sends it to Kafka | Claude | TODO |
| T6.6 | Paid, ship, cancel | Fake "Mark as paid"; ship confirms the reservation and reduces `on_hand`; cancel publishes `order.cancelled` and releases stock | Claude | TODO |
| T6.7 | Frontend | Order detail shows live status (polling is enough); error and rejected states are clear | Claude | TODO |
| T6.8 | Tests and review | Unit tests for saga logic; one end-to-end run through Docker; you test the happy path and the failure path | Claude + You | TODO |

## 10. Phase 7: Purchase orders (about 2 days)

| ID | Task | Acceptance criteria | Owner | Status |
|---|---|---|---|---|
| T7.1 | Purchase order API | `purchase_orders`, `purchase_order_items`; statuses DRAFT, SENT, RECEIVED | Claude | TODO |
| T7.2 | Receive goods | Receiving publishes an event; inventory-service increases stock and records an IN movement | Claude | TODO |
| T7.3 | Frontend | Purchase Order list and detail with "Receive goods" | Claude | TODO |

## 11. Phase 8: notification-service (about 2 days)

| ID | Task | Acceptance criteria | Owner | Status |
|---|---|---|---|---|
| T8.1 | Service and consumers | Consumes `stock.low` and order events; saves to `notifications`; idempotent | Claude | TODO |
| T8.2 | Low-stock event | inventory-service publishes `stock.low` when available stock falls under the reorder level | Claude | TODO |
| T8.3 | Frontend | Notification Center, bell badge with unread count, mark as read | Claude | TODO |

## 12. Phase 9: Dashboard, reports and polish (about 3 days)

| ID | Task | Acceptance criteria | Owner | Status |
|---|---|---|---|---|
| T9.1 | Dashboard | KPIs, sales chart, top products, recent orders, low stock, using real data | Claude | TODO |
| T9.2 | Reports | Sales report and inventory valuation (FIFO using `stock_batches`); Excel export | Claude | TODO |
| T9.3 | Full-stack Docker | One `docker compose up` runs everything; seed data script for a demo | Claude | TODO |
| T9.4 | Documentation | README with architecture diagram, how to run, screenshots; short demo script | Claude | TODO |
| T9.5 | Final review | Coverage check, cleanup, demo rehearsal | You | TODO |

## 13. How to start with Claude Code

1. Open the `inventory-platform` folder in VS Code and start Claude Code.
2. Paste this prompt:

   > Read TASKS.md and the Working agreement. Start with T0.1 only. When done, set the status to REVIEW and give me a short summary of what you changed and how I can check it. Do not start the next task.

3. Review the result, set the task to `DONE`, and tell Claude which task to do next.

Tip: if a task feels too big, ask Claude to split it into smaller ones before starting.
