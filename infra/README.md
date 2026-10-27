# infra

Local infrastructure for Stockline, run with Docker Compose: MySQL 8.4 and Kafka 4.1 (KRaft mode), plus an optional Kafka UI and the services as containers.

## Start and stop

Run from this folder (`infra/`):

```bash
docker compose up -d                   # MySQL + Kafka
docker compose --profile tools up -d   # also Kafka UI
docker compose ps                      # both should show "(healthy)"
docker compose logs -f kafka           # follow a container's logs
docker compose down                    # stop MySQL + Kafka, keep data (service containers: see "Services")
docker compose down -v                 # stop and delete all data (schemas are recreated on next start)
```

Settings come from `.env` in this folder. Copy `.env.example` to `.env` to change passwords or ports; without it, the local-dev defaults below are used.

## MySQL

| Setting | Value |
|---|---|
| Host / port | `localhost` / `3306` |
| App user | `stockline` / `stockline` (access to all service schemas) |
| Root user | `root` / `root` |
| Schemas | `auth_db`, `product_db`, `inventory_db`, `order_db`, `notification_db` |

**MySQL Workbench:** Database → Manage Connections → New. Use Hostname `127.0.0.1`, Port `3306`, Username `stockline`, then "Store in Vault…" for the password, and Test Connection.

The schemas are created by `mysql-init/01-schemas.sql`. MySQL runs scripts in that folder **only the first time**, when the data volume is empty. If you change the script, run `docker compose down -v` and start again. Tables are not created here; each service creates its own with Flyway.

## Kafka

| Who connects | Bootstrap server |
|---|---|
| A service running on your machine (IDE, `mvnw`) | `localhost:9092` |
| A service running in a container on this compose network | `kafka:29092` |

Kafka needs two addresses because a client first asks the broker where to connect, and the broker answers with an "advertised" address. `localhost` only works from your machine, and `kafka` only works inside the Docker network, so the broker advertises a different one on each listener.

Kafka UI (with `--profile tools`): http://localhost:8090

Useful commands:

```bash
docker exec stockline-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
docker exec stockline-kafka /opt/kafka/bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic order.created --from-beginning
```

In Git Bash, put `MSYS_NO_PATHCONV=1` in front of these commands, or Git Bash rewrites `/opt/...` into a Windows path. PowerShell and cmd don't have this problem.

## Services

The services can also run as containers. They are in the compose profile `services`, so a plain `docker compose up -d` doesn't start them: you can keep running a service from the IDE on its usual port without a clash. Stop the IDE run before starting the container (same port).

```bash
docker compose --profile services up -d --build   # build the images and start MySQL, Kafka and the services
docker compose ps                                 # wait for "(healthy)"
docker compose logs -f product-service            # follow a service's logs
docker compose --profile services stop product-service   # stop only the service container...
docker compose --profile services rm -f product-service  # ...and remove it; MySQL and Kafka keep running
docker compose --profile services down            # stop everything: services + MySQL + Kafka (data stays in volumes)
```

A plain `docker compose down` doesn't stop containers of a profile that isn't named, so the service containers would keep running. Don't use `docker compose rm -sf product-service` to stop one service: it also selects `stockline-mysql`.

`--build` rebuilds the images from the current code; leave it out to start the last built image. The build context is the repo root (each image also builds `libs/stockline-common`); see the service's README, section "Docker".

| Service | Container | Port (host, `.env`) | Database |
|---|---|---|---|
| product-service | `stockline-product-service` | `8082` (`PRODUCT_SERVICE_PORT`) | `product_db` |

Inside the compose network a service reaches MySQL as `mysql:3306` and Kafka as `kafka:29092`; the compose file sets `DB_URL` etc. accordingly. A containerized service uses the same schema as a run from the IDE, so data is shared between the two.
