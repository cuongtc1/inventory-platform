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
| Frontend | React + Vite + **TypeScript** (strict) + Ant Design + React Router + Axios; Vite dev proxy `/api` → backend (no CORS in services) |
| Tests | JUnit 5 + Mockito, Spring slice tests (`@WebMvcTest`, `@DataJpaTest`), Testcontainers (MySQL) for tests that need a database |
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
├── libs/            stockline-common: shared library (errors, messages, auditing, utils), own pom.xml and mvnw
├── frontend/        React + Vite + TypeScript app (rules: docs/conventions.md section 10)
├── infra/           docker-compose.yml, mysql-init/
├── docs/            mockup/ (Stockline HTML mockup), conventions.md
├── .history/        one learning summary per finished task (T1-1.md, T1-2.md, ...)
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
docker compose --profile services up -d --build   # also build and run the services as containers (product-service :8082)
docker compose down                    # add -v to wipe data; add --profile services to stop the service containers too
# MySQL app user: stockline/stockline. Kafka bootstrap: localhost:9092 from host, kafka:29092 from containers.

# Shared library (from libs/stockline-common/) - install before building any service, and after every change
./mvnw install                         # runs its tests, copies the jar to ~/.m2

# A backend service (from services/<name>/), e.g. product-service on :8082
./mvnw spring-boot:run                 # needs infra running
./mvnw test                            # needs Docker running (Testcontainers starts its own MySQL)
./mvnw clean package
# Health: /actuator/health   Swagger UI: /swagger-ui.html

# Frontend (from frontend/)
npm install                            # needs npm 10+ (`npm -v`); see frontend/README.md
npm run dev                            # http://localhost:5173, /api is proxied to the backend
npm run lint
npm run build                          # type check + production build
```

On Windows, use `mvnw.cmd`. `JAVA_HOME` must point to JDK 21 before running Maven.

New services follow `services/product-service` as the template: Spring Boot 4.1 from Initializr (YAML config), springdoc 3.x, the `stockline-common` dependency plus `spring.messages.basename: messages,common-messages`, Testcontainers MySQL for tests via `TestcontainersConfiguration` + `@ServiceConnection`, Mockito loaded as a `-javaagent` in Surefire. The developer knows Java 8/11 and Spring Boot 2: point out what is new in Java 21 / Boot 4 (libraries, packages, syntax) when it first appears.

## Workflow rules

- **One task at a time.** Work only on the task the developer names. Do not start the next task, even if it looks small.
- **Branch per task:** `feature/<task-id>-short-name` (for example `feature/T1.3-category-crud`). Never commit directly to `main`. Never merge or push without being asked.
- **Status:** move a task to `IN PROGRESS` when starting and `REVIEW` when finished. Never mark a task `DONE`; only the developer does that.
- **Finish every task with a short summary:** what changed, how to run it, what to check.
- **History file when a task is DONE** (from T1.1 on): when the developer marks a task `DONE`, write `.history/T<phase>-<n>.md` (e.g. `.history/T1-2.md` for T1.2) and commit it together with the `DONE` status change, on the task's branch, as `T1.2: Add history and mark task as done`. Content, following `.history/T1-1.md`: task info (branch, commit, dates, acceptance criteria), goal, steps done, problems and fixes, what's new vs Java 8/11 and Boot 2, concepts explained (including Q&A from the conversation), files changed, how it was verified, how to run it, next task.
- **Definition of Done:** it builds, tests pass, acceptance criteria are met, README/docs updated if behavior changed, committed with a clear message.
- **Ask when a requirement is unclear** instead of guessing. If a task is too big, propose splitting it before starting.
- This is a learning project: explain new concepts briefly (Spring, JPA, Kafka, React) when they first appear, and keep code readable over clever.

## Do

- Follow [docs/conventions.md](docs/conventions.md) for packages, URLs, JSON, errors, pagination, database naming, Kafka events, config, tests and Git. Update that file when a convention changes.
- Keep each service independent: its own `pom.xml`, Maven wrapper, `application.yml`, Flyway scripts, Dockerfile and tests.
- Put cross-cutting code (errors, messages, auditing, time/money/validation utils, paging) in `libs/stockline-common` and use it from every service; never put domain code there. After changing the library: its tests, `mvnw install`, then every service's tests.
- Use the layered structure in every service: `controller` / `service` / `repository` / `entity` / `dto` (plus `mapper`, `constant`, `config`, `event` as needed). Base package `com.stockline.<service>`.
- Return DTOs from controllers, never JPA entities. Validate input with Bean Validation (`@Valid`).
- Use the `GlobalExceptionHandler` from `stockline-common` (registered automatically) for the JSON error format from `docs/conventions.md` (400 validation, 404 not found, 409 conflict). Throw the library's `ApiException` subclasses with a message key.
- Decide shared rules before the second copy appears (see `docs/conventions.md`: enums in `constant/`, `Instant` + `Clock`, Asia/Ho_Chi_Minh business zone, USD `BigDecimal`, `CommonValidationUtils`, audit columns, hard delete).
- Change the database only through new Flyway migrations (`V<n>__description.sql`). Set `spring.jpa.hibernate.ddl-auto=validate`.
- Communicate between services through REST (via the service's API) or Kafka events only.
- Make Kafka consumers idempotent (`processed_events`), publish events through the outbox table, and use optimistic locking (`@Version`) on stock rows.
- Enforce role rules in the backend; frontend role checks are only for hiding UI.
- Read secrets and environment-specific values from environment variables with safe local defaults; commit `.env.example`, never `.env`.
- Write unit tests for service logic and slice tests for controllers and repositories with every feature. Run `./mvnw test` (or `npm run lint` and `npm run build` for the frontend) before committing.
- Frontend: follow `docs/conventions.md` section 10 (typed API layer, one error handler, `utils/format.ts` for dates/money, enum labels in `constants/`, every UI text in `messages/texts.ts`).
- Match the mockup's layout and labels when building frontend pages.
- Use clear commit messages in imperative mood, prefixed with the task id, e.g. `T1.3: Add category CRUD API`.

## Don't

- Don't edit a service's code while working on a task for another service, unless the task explicitly requires both.
- Don't add foreign keys or joins across schemas, and don't let one service read another service's database.
- Don't edit a Flyway migration that has already been committed; add a new one.
- Don't commit secrets, passwords, JWT signing keys, `.env` files, build output (`target/`, `dist/`, `node_modules/`) or IDE files.
- Don't use Java features newer than 21, preview features, or a different build tool than Maven.
- Don't add libraries or infrastructure beyond the stack above (Lombok and MapStruct are fine if added consistently; ask before anything else). Our own `stockline-common` is part of the stack.
- Don't add a refresh-token flow, a real payment provider, or features not in `TASKS.md`.
- Don't skip, disable or delete failing tests to make a build pass.
- Don't mark tasks `DONE`, merge to `main`, push, or rewrite Git history without being asked.
