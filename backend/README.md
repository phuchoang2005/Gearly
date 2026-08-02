# Gearly — Backend

REST + realtime API for **Gearly**, a PC / computer-component store: catalog,
cart, checkout, orders, reviews, blog/static pages, an admin console, an AI
shopping assistant, and MoMo payments.

**Stack:** Spring Boot 3.4.3 · Java 21 · MongoDB · Spring Security (JWT) ·
WebSocket · springdoc/OpenAPI · langchain4j (GitHub Models).

---

## Architecture

Conventional layered Spring app under `com.dominator.gearly`:

| Package | Responsibility |
|---|---|
| `controller/` (`admin/`, `user/`) | Thin HTTP layer — returns typed `ResponseEntity<DTO>`, never entities. |
| `service/` (`admin/`, `user/`, `common/`) | Business logic; one responsibility per service. |
| `mapper/` | Hand-written `@Component` entity ↔ DTO mappers. |
| `repository/` (`custom/`) | Spring Data Mongo repositories + custom aggregation impls. |
| `model/` | MongoDB documents (`products`, `orders`, `carts`, `users`, …). |
| `dto/` | Request/response payloads. |
| `exception/` | `ApiException` hierarchy + `GlobalExceptionHandler` (uniform `ErrorResponse`). |
| `security/` | JWT filter/util, `SecurityConfig`; `/api/admin/**` requires `ROLE_ADMIN`. |
| `ai/` | Intent routing + GitHub Models client for the shopping assistant. |
| `websocket/` | Chat WebSocket endpoint (`/ws-chat/**`). |
| `config/` | CORS, OpenAPI, Mongo config. |

Error handling is centralized: services throw typed exceptions
(`ResourceNotFoundException`, `BadRequestException`, `ConflictException`, …) and
`GlobalExceptionHandler` renders them as a uniform JSON body
(`{ timestamp, status, error, fieldErrors? }`).

---

## Requirements

- **JDK 21.** Building on a newer JDK (e.g. 26) fails with
  `ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN`
  because Lombok's annotation processor doesn't support it. Point Maven at a 21
  JDK: `export JAVA_HOME="$(/usr/libexec/java_home -v 21)"`.
- Maven 3.9+ (system `mvn`; the `./mvnw` wrapper is not checked in).
- Docker (for the containerized stack and the integration tests). **Colima**
  works — `colima start`, then `docker context use colima`.

---

## Environment variables

Secrets are **not** committed — they are read from environment variables. Copy
the template and fill it in:

```shell
cp .env.example .env   # then edit .env
```

Variables without a default in `application.properties` are **required**; the
app fails to boot loudly if they are unset (intentional). Required:
`JWT_SECRET`, `GOOGLE_CLIENT_ID`, `MAIL_USERNAME`/`MAIL_PASSWORD`,
`MOMO_ACCESS_KEY`/`MOMO_SECRET_KEY`. Optional (have defaults):
`MAIL_HOST`/`MAIL_PORT`, `MOMO_PARTNER_CODE`/`MOMO_RETURN_URL`/`MOMO_NOTIFY_URL`,
`GITHUB_MODELS_TOKEN`/`GITHUB_MODELS_SECOND_TOKEN` (AI is inert without a token),
`CORS_ALLOWED_ORIGINS`, `SPRING_DATA_MONGODB_URI`. See
[`.env.example`](.env.example) for the full list.

---

## Running

A [`Makefile`](Makefile) wraps the common flows (`make help` lists them). It uses
Docker for the stack and pins local Maven to JDK 21.

### Docker (recommended)

```shell
make env      # create .env from .env.example (then fill in secrets)
make up       # build + start MongoDB + the Spring app (http://localhost:8080)
make seed     # load the sample catalog into MongoDB
make logs     # tail the app logs
make down     # stop (keep data);  make clean  also drops the data volume
```

`make dev` runs the hot-reload stack (`docker-compose.dev.yml`, source-mounted).

### Local Maven (needs a local MongoDB)

```shell
export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
set -a; source .env; set +a     # load env vars into the shell
make run                         # or: mvn spring-boot:run
```

### Seeding / migrating the database

Sample data lives in [`data/seed/`](data/seed/).

- `make seed` — drops & reloads every collection from `data/seed/gearly.*.json`
  (products, users, orders, reviews, categories, cities/states/countries, …).
- `make migrate` — idempotent in-place migration of an existing **Bookify**-shaped
  DB → Gearly (`books`→`products`, nested `bookId`→`productId`).
- `make mongosh` — open a shell on the `gearly` database.

---

## API docs (Swagger / OpenAPI)

With the app running:

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Public routes (auth, catalog reads, webhooks, `/ws-chat/**`, the docs
endpoints) are open; everything under `/api/admin/**` requires an admin JWT.

---

## Testing

```shell
make test        # mvn test, pinned to JDK 21
```

The suite is safe to run **offline**: unit tests (services, mappers) and
`@WebMvcTest` slices (security + `GlobalExceptionHandler`) need no infrastructure.
The context-load test (`GearlyApplicationTests`) and the analytics aggregation
test (`OrderAnalyticsServiceIntegrationTest`) spin up a throwaway MongoDB via
**Testcontainers** and are **Docker-gated** (`@Testcontainers(disabledWithoutDocker
= true)`) — they self-skip when no Docker daemon is reachable, and run for real
when Docker/Colima is up (`Skipped: 0` then). Ryuk is disabled in the Surefire
config (it can't bind-mount Colima's socket).

### Running the container-backed tests on Colima

Testcontainers' bundled docker-java client doesn't auto-read the Docker CLI
context and defaults to an API version modern engines reject, so point it at
Colima explicitly with two per-developer files in your home directory:

`~/.testcontainers.properties` — where the daemon is:

```properties
docker.host=unix:///Users/<you>/.colima/default/docker.sock
```

`~/.docker-java.properties` — a supported API version (Docker 25+/Colima require ≥ 1.44):

```properties
api.version=1.44
```

Then `colima start` and `make test` runs the full suite with `Skipped: 0`.
Without these files the container tests simply skip and the rest still passes.
