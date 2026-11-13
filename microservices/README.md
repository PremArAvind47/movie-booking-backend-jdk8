# Cinema microservices (Block 2: steps 9 to 13, production style)

A small but production-shaped Spring Cloud system: 3 business services, 3 platform services.
**No database and no AI on purpose**: the data is hardcoded and kept in memory, so you can study
the microservice parts only. It lives next to the AI app of this repo but is a **separate project**
(own `pom.xml`, Spring Boot 4.0.9 + Spring Cloud 2025.1.4, Java 21).

## The picture

```
                  client (browser, app, curl)
                              |
                              v
                     +-----------------+   "where is booking-service?"    +--------------------+
                     |   api-gateway   |--------------------------------->| discovery-server   |
                     |   :8080         |                                   | (Eureka)   :8761   |
                     +-----------------+                                   +--------------------+
            /api/v1/movies /      \ /api/v1/bookings                         ^   ^   ^   ^
                          v        v                                         |   |   |   |  every service
              +---------------+   +------------------+   Feign              |   |   |   |  registers itself
              | movie-service |<--| booking-service  |---------+            |   |   |   |
              |   :8081       |   |   :8082          |         v            |   |   |   |
              +---------------+   +------------------+   +-----------------+    (all services)
                  (internal:              (the boss of      | payment-service |
               /reservations)             a booking)        |  :8083 internal |
                                                            +-----------------+
              +----------------+
              | config-server  |  gives every service its settings from config-repo/
              |   :8888        |
              +----------------+
```

| App | Port | Job |
|---|---|---|
| `config-server` | 8888 | keeps the settings of all services (`config-repo/`) |
| `discovery-server` | 8761 | Eureka: the phone book (services register, others look them up by name) |
| `api-gateway` | 8080 | the only front door; adds the request id; routes `/api/v1/movies/**` and `/api/v1/bookings/**` |
| `movie-service` | 8081 | movies, free seats, reserve / release seats |
| `booking-service` | 8082 | creates bookings: calls movie-service and payment-service with OpenFeign |
| `payment-service` | 8083 | approves or declines a payment (hardcoded rule) |
| `platform-starter` | (library) | shared code: request id filter, one error format. No business logic |

## How a booking works (orchestration)

`booking-service` is the boss. For `POST /api/v1/bookings`:

1. ask movie-service for the movie and price
2. reserve the seats (movie-service)
3. charge the payment (payment-service)
4. if the payment is **declined** or payment-service is **down**: **release the seats** (the "undo", called compensation)
5. save the booking: `CONFIRMED`, `PAYMENT_DECLINED`, `FAILED`, or `NEEDS_ATTENTION` (even the undo failed)

Hardcoded rules to play with: Avengers 250, Batman 200, Dune 300 rupees per seat. Free seats: 42, 7, 120.
A payment above 1000 rupees is declined (for example 5 seats of Dune = 1500).

## Run it

**With Docker (one command):**
```
cd microservices
docker compose up --build
```
The gateway is at http://localhost:8080 (set `GATEWAY_PORT=9090` first if port 8080 is used by the AI app).
Eureka dashboard: http://localhost:8761.

**Without Docker** (needs Java 21 and Maven; on Windows use Git Bash for the scripts):
```
cd microservices
mvn package -DskipTests
./scripts/run-local.sh        # starts the 6 apps in the right order
./scripts/smoke-test.sh       # 12 end-to-end checks through the gateway
./scripts/stop-local.sh
```

## Things to try

```
curl localhost:8080/api/v1/movies
curl -H "Content-Type: application/json" -d '{"movieId":2,"seats":2}' localhost:8080/api/v1/bookings
curl localhost:8080/api/v1/bookings
```

1. **Payment declined, seats come back.** Book 5 seats of Dune (`"movieId":3,"seats":5`): HTTP 402. Check `GET /api/v1/movies/3`: still 120 free seats.
2. **A service dies.** Stop payment-service (`docker compose stop payment-service`). Book: HTTP 503 `PAYMENT_SERVICE_UNAVAILABLE`, and the seats are released. After a few failures the **circuit opens** and answers fail in about 25 ms (no waiting). Start it again: bookings work again by themselves.
3. **Gateway when a service is gone.** Stop movie-service and call `GET /api/v1/movies`: HTTP 503 in the standard error format.
4. **Change a setting without a restart.** Edit `config-repo/booking-service.yml` (`booking.max-seats-per-booking: 2`), then `curl -X POST <booking-service>:8082/actuator/refresh` (with Docker the business services are not published; use `docker compose exec booking-service wget -qO- --post-data= http://localhost:8082/actuator/refresh`). Booking 3 seats now gives `TOO_MANY_SEATS`.
5. **Follow one request through all services.** `curl -H "X-Request-Id: my-test-1" ...` then search the logs: `docker compose logs | grep my-test-1` shows the same id in gateway, booking, movie and payment.
6. **Internal APIs are hidden.** `GET /api/v1/reservations/x` and `POST /api/v1/payments` at the gateway: 404 `ROUTE_NOT_FOUND`.

## What is "production style" here, and where to find it

| Practice | Where |
|---|---|
| Service discovery by name, no host or port in the code | `discovery-server`, `@FeignClient(name = "movie-service")`, `lb://` routes in `config-repo/api-gateway.yml` |
| Central settings, change without restart | `config-server`, `config-repo/`, `@RefreshScope` in `BookingRules` and `PaymentRules` |
| One public front door, internal APIs hidden | `config-repo/api-gateway.yml` (only 2 routes) and Docker ports (only the gateway is published) |
| Circuit breaker, timeouts, fallback | `ResilienceConfig`, `config-repo/booking-service.yml`, `*FallbackFactory` |
| Retries only where safe | Feign retry works because every call is **idempotent** (booking id as key); the gateway retries **GET only** |
| Undo of a half-done booking (saga) | `BookingService.fail(...)` |
| Business errors do not trip the circuit breaker | `ignoreExceptions(ApiException.class)` + `DownstreamErrorDecoder` (4xx = answer, 5xx = failure) |
| Request id across services and in every log line | `RequestIdGlobalFilter` (gateway), `RequestIdFilter` (platform-starter), log pattern in `config-repo/application.yml` |
| One error JSON format everywhere (also from the gateway) | `GlobalExceptionHandler`, `GatewayErrorHandler` |
| Health checks, graceful shutdown | Actuator `/actuator/health/readiness`, `server.shutdown: graceful`, Docker `healthcheck` and start order |
| Small container, not root, memory aware | `Dockerfile` |
| Versioned URLs `/api/v1/...`, validation, no stack trace to the client | controllers, `platform-starter` |
| Fast tests of the logic, no network | `*Test.java` (25 tests) |

## Demo settings you must change for production

- Eureka: `enable-self-preservation: false`, `use-read-only-response-cache: false`, short leases and fetch intervals (`config-repo/application.yml`, `discovery-server`). They make changes visible in seconds. In production keep the safe defaults.
- Spring Cloud LoadBalancer cache `ttl: 5s` (default 35 s).
- Ports 8761 and 8888 are published only for learning.

## What is NOT here (on purpose) and what a real company adds

- **Security**: no login. Real systems add authentication and authorization at the gateway (JWT / OAuth2) and between services. That is Block 2 step 24 (Spring Security). The Config Server and Eureka must also be protected.
- **Databases**: each service gets its own database. Data here is in memory and is lost on restart.
- **Messaging** (Kafka) for choreography and events, **Redis** for caching and rate limiting (Block 2 step 15).
- **Metrics and tracing dashboards** (Prometheus, Grafana, distributed tracing): Block 2 step 18.
- **Kubernetes**: on Kubernetes many companies use its own Services and ConfigMaps instead of Eureka and Config Server. This project uses Spring Cloud on purpose, to learn those parts.
- Several copies of each service, a shared library repository, contract tests, a CI pipeline for these modules.

## Honest status

- Checked as plain Java processes: all 25 unit tests, the 12 smoke checks, the outage demos and the live settings change.
- `Dockerfile` and `docker-compose.yml` were checked for syntax (`docker compose config`) but **not started** where this was written (no Docker engine there). If `docker compose up --build` shows a problem, that is the first place to look.
- I do not know the internal systems of large companies. This follows widely used industry patterns.
