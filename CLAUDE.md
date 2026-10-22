# CLAUDE.md

Guidance for Claude Code in this repository. Read this file and `TASKS.md` before starting any work.

## Project summary

**Stockline** is a learning project: a microservices inventory and order platform. Claude acts as tech lead and does most of the implementation; the developer reviews every task before the next one starts. `TASKS.md` is the source of truth for scope, task order, acceptance criteria and status.

Business scope: product catalog (categories, suppliers, products), warehouses and stock levels, stock movements and low-stock alerts, sales orders with a Kafka-based stock-reservation saga, purchase orders and goods receipt, notifications, dashboard and reports, and role-based access (STAFF < MANAGER < ADMIN).

## Tech stack

| Area | Choice |
|---|---|
| Backend | **Java 21** (JDK 21.0.12.1; do not use JDK 25 or 14 features/toolchains), Spring Boot (latest stable that supports Java 21), Maven wrapper (`mvnw`), Spring Data JPA / Hibernate |
| Database | MySQL 8 in Docker, one schema per service, Flyway migrations |
| Messaging | Kafka in KRaft mode (no Zookeeper), Spring Kafka |
| Gateway | Spring Cloud Gateway, validates JWT, handles CORS |
| Auth | Plain JWT (no refresh token), BCrypt passwords, `user_type` = STAFF / MANAGER / ADMIN stored as a string |
| Frontend | React + Vite + Ant Design + React Router + Axios |
| Tests | JUnit 5 + Mockito, Spring slice tests (`@WebMvcTest`, `@DataJpaTest`) |
| API docs | springdoc-openapi (Swagger UI) per service |

### Services and ports

| Service | Port | Schema |
|---|---|---|
| api-gateway | 8080 | - |
| auth-service | 8081 | `auth_db` |
| product-service | 8082 | `product_db` |
| inventory-service | 8083 | `inventory_db` |
| order-service | 8084 | `order_db` |
| notification-service | 8085 | `notification_db` |
| MySQL | 3306 | |
| Kafka | 9092 | |
| frontend (Vite) | 5173 | |

Kafka topics: `order.created`, `stock.reserved`, `stock.failed`, `order.cancelled`, `stock.low`.

Order status flow: `PENDING → RESERVED → PAID → SHIPPED → COMPLETED`, plus `REJECTED` (stock failed) and `CANCELLED` (only before shipping).

## Repository layout (target)

```
inventory-platform/
├── services/        one folder per Spring Boot service, each with its own pom.xml and mvnw
├── frontend/        React + Vite app
├── infra/           docker-compose.yml, mysql-init/
├── docs/            mockup/ (Stockline HTML mockup), conventions.md
├── TASKS.md
├── CLAUDE.md
└── README.md
```

The HTML mockup (`index.html`, `app.js`, `style.css`) lives in `docs/mockup/`. Empty folders are kept in Git with a `.gitkeep` file; delete it once the folder has real content.

The mockup is the UI reference: dark collapsible sidebar, white content area, top bar with avatar and Log out, tables with Edit/Delete icon buttons, status badges, and a role switch ("View as") that hides menu items by role.

## Commands

Fill these in as the pieces are created.

```bash
# Infra (from infra/) - details in infra/README.md
docker compose up -d                   # MySQL + Kafka
docker compose --profile tools up -d   # also Kafka UI on :8090
docker compose down                    # add -v to wipe data
# MySQL app user: stockline/stockline. Kafka bootstrap: localhost:9092 from host, kafka:29092 from containers.

# A backend service (from services/<name>/)
./mvnw spring-boot:run
./mvnw test
./mvnw clean package

# Frontend (from frontend/)
npm install
npm run dev
npm run build
```

On Windows, use `mvnw.cmd`. `JAVA_HOME` must point to JDK 21 before running Maven.

## Workflow rules

- **One task at a time.** Work only on the task the developer names. Do not start the next task, even if it looks small.
- **Branch per task:** `feature/<task-id>-short-name` (for example `feature/T1.3-category-crud`). Never commit directly to `main`. Never merge or push without being asked.
- **Status:** move a task to `IN PROGRESS` when starting and `REVIEW` when finished. Never mark a task `DONE`; only the developer does that.
- **Finish every task with a short summary:** what changed, how to run it, what to check.
- **Definition of Done:** it builds, tests pass, acceptance criteria are met, README/docs updated if behavior changed, committed with a clear message.
- **Ask when a requirement is unclear** instead of guessing. If a task is too big, propose splitting it before starting.
- This is a learning project: explain new concepts briefly (Spring, JPA, Kafka, React) when they first appear, and keep code readable over clever.

## Do

- Follow [docs/conventions.md](docs/conventions.md) for packages, URLs, JSON, errors, pagination, database naming, Kafka events, config, tests and Git. Update that file when a convention changes.
- Keep each service independent: its own `pom.xml`, Maven wrapper, `application.yml`, Flyway scripts, Dockerfile and tests.
- Use the layered structure in every service: `controller` / `service` / `repository` / `entity` / `dto` (plus `mapper`, `exception`, `config`, `event` as needed). Base package `com.stockline.<service>`.
- Return DTOs from controllers, never JPA entities. Validate input with Bean Validation (`@Valid`).
- Use one global `@RestControllerAdvice` per service with the JSON error format from `docs/conventions.md` (400 validation, 404 not found, 409 conflict).
- Change the database only through new Flyway migrations (`V<n>__description.sql`). Set `spring.jpa.hibernate.ddl-auto=validate`.
- Communicate between services through REST (via the service's API) or Kafka events only.
- Make Kafka consumers idempotent (`processed_events`), publish events through the outbox table, and use optimistic locking (`@Version`) on stock rows.
- Enforce role rules in the backend; frontend role checks are only for hiding UI.
- Read secrets and environment-specific values from environment variables with safe local defaults; commit `.env.example`, never `.env`.
- Write unit tests for service logic and slice tests for controllers and repositories with every feature. Run `./mvnw test` (or `npm run build` for the frontend) before committing.
- Match the mockup's layout and labels when building frontend pages.
- Use clear commit messages in imperative mood, prefixed with the task id, e.g. `T1.3: Add category CRUD API`.

## Don't

- Don't edit a service's code while working on a task for another service, unless the task explicitly requires both.
- Don't add foreign keys or joins across schemas, and don't let one service read another service's database.
- Don't edit a Flyway migration that has already been committed; add a new one.
- Don't commit secrets, passwords, JWT signing keys, `.env` files, build output (`target/`, `dist/`, `node_modules/`) or IDE files.
- Don't use Java features newer than 21, preview features, or a different build tool than Maven.
- Don't add libraries or infrastructure beyond the stack above (Lombok and MapStruct are fine if added consistently; ask before anything else).
- Don't add a refresh-token flow, a real payment provider, or features not in `TASKS.md`.
- Don't skip, disable or delete failing tests to make a build pass.
- Don't mark tasks `DONE`, merge to `main`, push, or rewrite Git history without being asked.
