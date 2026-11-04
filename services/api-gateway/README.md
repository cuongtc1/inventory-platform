# api-gateway

The one entrance to Stockline. Spring Cloud Gateway (WebMVC), Spring Boot 4.1, Java 21, port **8080**. No database and no controllers: it checks each request and forwards it to the service that owns the URL.

```
browser ──▶ api-gateway :8080 ──▶ auth-service :8081, product-service :8082, ...
            1. CORS
            2. valid JWT? (except login)
            3. route by URL
```

The gateway checks **who** is calling. **What they may do** is decided by each service (T3.4). Until T3.4, product-service on :8082 doesn't check tokens itself, so only requests through the gateway are protected.

## Run

```bash
cd libs/stockline-common && ./mvnw install     # after every change to the library
cd services/api-gateway
./mvnw spring-boot:run                         # Windows cmd/PowerShell: mvnw.cmd spring-boot:run
./mvnw test                                    # no Docker needed
```

Start the services it forwards to as well (`auth-service`, `product-service`). Health check: http://localhost:8080/actuator/health. As containers, everything together (from `infra/`): `docker compose --profile services up -d --build`.

Try it:

```bash
curl -i localhost:8080/api/products                       # 401
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
     -d '{"email":"admin@stockline.local","password":"admin12345"}'      # copy accessToken
curl -i localhost:8080/api/products -H "Authorization: Bearer <accessToken>"   # 200
```

The frontend still calls product-service directly (Vite proxy) until it can log in; T3.5 switches it to the gateway. There is no Swagger UI here: use each service's own.

## Routes

In `application.yml`. A request goes unchanged (same path, query, body and headers, including `Authorization`) to the first route that matches.

| Paths | Service | Target |
|---|---|---|
| `/api/auth/**`, `/api/users/**` | auth-service | `AUTH_SERVICE_URL`, `http://localhost:8081` |
| `/api/categories/**`, `/api/suppliers/**`, `/api/products/**` | product-service | `PRODUCT_SERVICE_URL`, `http://localhost:8082` |
| `/api/warehouses/**`, `/api/stock-levels/**`, `/api/stock-movements/**` | inventory-service | `INVENTORY_SERVICE_URL`, `http://localhost:8083` |
| `/api/customers/**`, `/api/sales-orders/**`, `/api/purchase-orders/**` | order-service | `ORDER_SERVICE_URL`, `http://localhost:8084` |
| `/api/notifications/**` | notification-service | `NOTIFICATION_SERVICE_URL`, `http://localhost:8085` |

The routes of later phases are there already; those services answer 503 until they exist.

## Who gets through

| Request | Without a token | With a valid token |
|---|---|---|
| `POST /api/auth/login` | Forwarded | Forwarded |
| `/actuator/health`, `/actuator/info` | Answered here | Answered here |
| Any other URL | `401 UNAUTHORIZED` | Forwarded |

A token is valid when its signature matches `JWT_SECRET` (HS256), it hasn't expired, and `iss` is `stockline-auth`: the same check as auth-service (`JwtSecurity` in stockline-common).

## Errors

The gateway's own errors use the normal JSON (conventions section 3), through the library's `GlobalExceptionHandler`:

| Situation | Status | `code` |
|---|---|---|
| No token, a bad one, or an expired one | 401 | `UNAUTHORIZED` |
| No route for the URL (with a valid token) | 404 | `NOT_FOUND` |
| The service is down, or didn't answer within `GATEWAY_READ_TIMEOUT` | 503 | `SERVICE_UNAVAILABLE` |

Errors from a service (400, 403, 404, 409, ...) pass through unchanged. If one service is down, the routes of the others keep working.

## CORS

A browser blocks a page from calling another origin (scheme + host + port) unless the server allows it. Before a request with an `Authorization` header it first sends a **preflight**: an `OPTIONS` request without a token, asking "may I?". That's why CORS runs in the security chain, before the token check.

- Allowed origins: `CORS_ALLOWED_ORIGINS` (comma-separated), default `http://localhost:5173`. Any other origin gets 403.
- Allowed: GET, POST, PUT, PATCH, DELETE, the headers `Authorization`, `Content-Type`, `Accept`; `Location` is readable by the page; a preflight answer is remembered for 1 hour.
- Only the gateway does CORS, never the services (two `Access-Control-Allow-Origin` headers would make the browser reject the response).

## Configuration

| Variable | Default (local only) | Meaning |
|---|---|---|
| `SERVER_PORT` | `8080` | |
| `AUTH_SERVICE_URL`, `PRODUCT_SERVICE_URL`, `INVENTORY_SERVICE_URL`, `ORDER_SERVICE_URL`, `NOTIFICATION_SERVICE_URL` | `http://localhost:8081` ... `8085` | Where each service is |
| `JWT_SECRET` | `local-dev-only-stockline-jwt-secret-change-me` | At least 32 bytes; **must equal auth-service's** |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | See above |
| `GATEWAY_READ_TIMEOUT` | `30s` | How long to wait for a service's answer |

## Tests

`./mvnw test`: 13 tests in `GatewayIntegrationTest`; no Docker needed, because there is no database. A real gateway runs on a random port and a real HTTP client calls it. The services are fake: small servers from the JDK (`com.sun.net.httpserver`) that remember what they received. Tokens are really signed.

Checked: login without a token, body and token forwarded, every path to its service, service errors passed through, no token, wrong secret and expired token (401, and the service sees nothing), unknown path, too slow a service (503), preflight from the frontend and from another origin, one CORS header on a real response, open health check.

## Files

| Path | Purpose |
|---|---|
| `src/main/resources/application.yml` | The routes and all settings |
| `config/SecurityConfig` | JWT check, open URLs, CORS |
| `config/JwtProperties`, `config/CorsProperties` | Typed settings |
| `Dockerfile` | Same structure as product-service's |

## New compared to Spring Boot 2.x / Java 8–11

| Old | Now | Notes |
|---|---|---|
| Spring Cloud Gateway = reactive only (WebFlux, Netty) | Also a servlet variant: `spring-cloud-starter-gateway-server-webmvc` | We use it because every other service is Spring MVC, so the same security style and error handler work here |
| `spring.cloud.gateway.routes` | `spring.cloud.gateway.server.webmvc.routes` | The properties moved when the two variants were separated |
| Spring Cloud versions like `Hoxton` | `2025.1.x` (year + number) | A BOM in `dependencyManagement`. Each line fits certain Boot versions: 2025.1.x fits Boot 4.0 and 4.1 |
| A thread per waiting request, or reactive code to avoid that | `spring.threads.virtual.enabled: true` (Java 21 virtual threads, Boot 3.2+) | A waiting request doesn't hold a real thread, and the code stays plain |
| `RestTemplate` timeouts set in code | `spring.http.clients.connect-timeout` / `read-timeout` | Boot's settings for its HTTP clients |
