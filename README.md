# Stockline: Inventory & Order Platform

A learning project that builds an inventory and order management system as a set of Spring Boot microservices with a React frontend. It covers a layered REST service, JWT authentication and role-based access, an API gateway, Kafka events, and a stock-reservation saga with optimistic locking, idempotent consumers and the outbox pattern.

> **Status:** Phase 0 (setup). See [TASKS.md](TASKS.md) for the full plan and the status of each task.

## Features (planned)

- **Catalog:** categories, suppliers, products (unique SKU, search, filters, pagination)
- **Inventory:** warehouses, stock levels, stock movements, low-stock alerts
- **Sales orders:** customers, orders, status flow with stock reservation through Kafka
- **Purchase orders:** create, send, receive goods into stock
- **Notifications:** low-stock and order events, unread badge
- **Dashboard and reports:** KPIs, sales chart, inventory valuation, Excel export
- **Users and roles:** STAFF < MANAGER < ADMIN (a higher role includes the lower ones)

## Architecture

```
                    ┌──────────────────────┐
  React frontend ──▶│  api-gateway  :8080  │  JWT check, routing, CORS
     :5173          └──────────┬───────────┘
                               │ REST
     ┌─────────────┬───────────┼─────────────┬─────────────────┐
     ▼             ▼           ▼             ▼                 ▼
 auth-service  product-svc  inventory-svc  order-svc   notification-svc
    :8081        :8082        :8083         :8084          :8085
     │             │            │  ▲          │  ▲              ▲
     │             │            ▼  │          ▼  │              │
     │             │      ┌────────┴─────────────┴──────────────┴─┐
     │             │      │         Kafka :9092 (events)          │
     │             │      └───────────────────────────────────────┘
     ▼             ▼            ▼             ▼                 ▼
  auth_db     product_db   inventory_db    order_db      notification_db
            MySQL 8 :3306, one schema per service, no cross-schema foreign keys
```

Kafka topics: `order.created`, `stock.reserved`, `stock.failed`, `order.cancelled`, `stock.low`.

Order status flow: `PENDING → RESERVED → PAID → SHIPPED → COMPLETED`, plus `REJECTED` (not enough stock) and `CANCELLED` (before shipping).

## Tech stack

| Area | Technology |
|---|---|
| Backend | Java 21, Spring Boot, Spring Data JPA / Hibernate, Maven wrapper |
| Database | MySQL 8 (Docker), Flyway migrations |
| Messaging | Kafka in KRaft mode (Docker), Spring Kafka |
| Gateway and auth | Spring Cloud Gateway, JWT, BCrypt |
| Frontend | React, Vite, Ant Design, React Router, Axios |
| Tests | JUnit 5, Mockito, Spring slice tests |

## Repository layout

```
inventory-platform/
├── services/      Spring Boot services (one Maven project each)
│   ├── api-gateway/
│   ├── auth-service/
│   ├── product-service/
│   ├── inventory-service/
│   ├── order-service/
│   └── notification-service/
├── libs/
│   └── stockline-common/   shared library: errors, messages, auditing, time/money/validation utils
├── frontend/      React + Vite app
├── infra/         docker-compose.yml, mysql-init/ (schema init scripts)
├── docs/
│   ├── mockup/    Stockline HTML mockup (UI reference)
│   └── conventions.md   coding, API, database, Kafka and Git conventions
├── .history/      what was done and learned in each finished task
├── TASKS.md       plan, acceptance criteria, task status
└── CLAUDE.md      rules for Claude Code in this repo
```

## Prerequisites

- JDK 21 (`JAVA_HOME` pointing to it)
- Docker Desktop
- Node.js 20 or newer
- Git
- Editor: IntelliJ reads `.editorconfig` (tabs, UTF-8, LF) by itself; in VS Code install the "EditorConfig for VS Code" extension

## Getting started

Start MySQL and Kafka (see [infra/README.md](infra/README.md) for connection details and the optional Kafka UI):

```bash
cd infra
docker compose up -d
```

Install the shared library once, and again after every change to it (services can't compile without it):

```bash
cd libs/stockline-common
./mvnw install                # Windows: mvnw.cmd install
```

Run a service (needs JDK 21; see each service's README):

```bash
cd services/product-service
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

Or run it as a container, without JDK or Maven on your machine (the image builds the library itself; see [infra/README.md](infra/README.md#services)):

```bash
cd infra
docker compose --profile services up -d --build
```

Both ways use port 8082 and the same database, so run only one of them at a time.

| Service | Status | Health | Swagger UI |
|---|---|---|---|
| product-service | categories, suppliers, products API | http://localhost:8082/actuator/health | http://localhost:8082/swagger-ui.html |

The other services and the frontend don't exist yet; this section will grow as each task is completed.

To view the UI mockup, open `docs/mockup/index.html` in a browser.

## Workflow

Each task in [TASKS.md](TASKS.md) is done on its own branch `feature/<task-id>-short-name`, reviewed, then merged to `main`. Commit messages start with the task id, for example `T1.3: Add category CRUD API`. All code follows [docs/conventions.md](docs/conventions.md).
