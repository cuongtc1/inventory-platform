# infra

Local infrastructure for Stockline, run with Docker Compose: MySQL 8.4 and Kafka 4.1 (KRaft mode), plus an optional Kafka UI.

## Start and stop

Run from this folder (`infra/`):

```bash
docker compose up -d                   # MySQL + Kafka
docker compose --profile tools up -d   # also Kafka UI
docker compose ps                      # both should show "(healthy)"
docker compose logs -f kafka           # follow a container's logs
docker compose down                    # stop, keep data
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
