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
| `security/` | JWT filter/util, `SecurityConfig`; `/api/admin/**` requires `ROLE_ADMIN`, uploaded assets under `/uploads/**` are public. |
| `ai/` | Intent routing + GitHub Models client for the shopping assistant. |
| `websocket/` | Chat WebSocket endpoint (`/ws-chat/**`). |
| `config/` | CORS, OpenAPI, Mongo config. |

Error handling is centralized: services throw typed exceptions
(`ResourceNotFoundException`, `BadRequestException`, `ConflictException`, …) and
`GlobalExceptionHandler` renders them as a uniform JSON body
(`{ timestamp, status, error, fieldErrors? }`).

### Migration in progress: bounded contexts

The layered packages above are being restructured into **bounded contexts with rich
aggregates** over Sprints 8–13 — see
[`DDD_REFACTORING_PLAN.md`](../DDD_REFACTORING_PLAN.md). The target packages already
exist alongside the current ones, each with a `package-info.java` stating its
responsibility and its relationships on the context map:

| Package | Role |
|---|---|
| `shared/` | Shared kernel — value objects, typed ids, `AggregateRoot`, the Mongo/Jackson converters. |
| `ordering/`, `catalog/` | Core domain. |
| `cart/`, `reviews/`, `identity/`, `content/` | Supporting contexts. |
| `payments/`, `notification/`, `storage/`, `geo/`, `assistant/` | Generic subdomains, each behind a port. |
| `analytics/` | The query side — the only package permitted `MongoTemplate`. |
| `platform/` | Cross-cutting: config, security, exception handling. Not a context. |

Each context is layered `domain / application / infrastructure / api`. **The rules are
enforced, not documented:** `ArchitectureFitnessTest` (ArchUnit) fails the build on a
domain package importing web/security/HTTP types, a `@Document` outside a domain
package, a public setter on an aggregate, `MongoTemplate` outside `analytics`, or one
context reaching into another other than through a port, a `*Event` or a published
`*Snapshot`/`*Id`. The rules are currently **scoped to the new packages** (marked
`SCOPE:` in the test) so the untouched legacy packages don't fail them; those scopes
come off in S13.

---

## Transactions

**MongoDB must run as a replica set.** Multi-document transactions do not exist on a
standalone `mongod`, so `@Transactional` is silently inert without one — which is
exactly the bug S8 fixed: `createOrder` could save an order and then fail partway
through decrementing stock, leaving the order placed against stock only partially
reserved.

`docker-compose.yml` starts `mongod --replSet rs0`, and the container healthcheck
doubles as an idempotent one-shot `rs.initiate()` — it reports healthy only once the
node is a writable primary, and `spring-app` waits on `condition: service_healthy`.
Nothing extra to run:

```shell
make up            # or: make mongo-up  (waits for a primary, not just for the port)
make rs-status     # prints 1 when the set is healthy
```

The member is registered as `localhost:27017` so host-side tooling (mongosh, Compass,
`make run`) can follow the topology; containers therefore connect with
`directConnection=true`, which skips topology discovery but still permits transactions.
Both URIs in `application.properties` / `application-docker.properties` already carry it.

> **If you run your own MongoDB**, start it with `--replSet rs0` and run
> `rs.initiate()` once. Against a standalone server every transactional write now fails
> with *"Transaction numbers are only allowed on a replica set member or mongos"*. That
> is deliberate — a loud failure beats a silent half-write. Existing data directories
> work unchanged; `rs.initiate()` on a populated dbpath is safe.

`Product`, `Order` and `Cart` also carry a `@Version` field for optimistic locking,
which closes the read-then-write oversell race (two checkouts both reading stock 1 and
both writing 0). A lost race surfaces as **409**, not 500. The field is `@JsonIgnore`d —
it is an internal token, never on the wire and never client-settable.

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
  (products, users, orders, reviews, categories, cities/states/countries, …). The
  product dumps store `addedAt`/`modifiedAt` as Extended-JSON `$date`, so a fresh
  seed lands real `Date`s (matching `Product.addedAt : Instant`), and the
  products/orders/carts dumps carry `version: 0` — so a fresh seed needs no migrate
  step.
- `make migrate` — idempotent in-place migration of an existing **Bookify**-shaped
  DB → Gearly (`books`→`products`, nested `bookId`→`productId`), the
  `products.addedAt`/`modifiedAt` `String`→`Date` conversion, and the `version`
  backfill for legacy data.
- `make mongosh` — open a shell on the `gearly` database.

> **Run `make migrate` on any database seeded before Sprint 8.** Spring Data reads a
> missing `version` field as `null` and treats a null version as "not yet persisted",
> so the first write to a pre-S8 `product`/`order`/`cart` would be issued as an insert
> and fail on the duplicate `_id`. The migration sets `version: 0` where the field is
> absent; it is `$exists`-guarded, so re-runs skip documents that already have one and
> never reset a live counter.

---

## API docs (Swagger / OpenAPI)

With the app running:

- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

Public routes (auth, catalog reads, webhooks, `/ws-chat/**`, uploaded media at
`/uploads/**`, the docs endpoints) are open; everything under `/api/admin/**`
requires an admin JWT.

---

## Testing

```shell
make test        # mvn test, pinned to JDK 21
```

The suite is safe to run **offline**: unit tests (services, mappers), the ArchUnit
fitness functions, and `@WebMvcTest` slices (security + `GlobalExceptionHandler`) need
no infrastructure. The context-load test (`GearlyApplicationTests`), the analytics
aggregation test (`OrderAnalyticsServiceIntegrationTest`) and the transaction /
optimistic-locking test (`OrderPlacementTransactionIntegrationTest`) spin up a
throwaway MongoDB via **Testcontainers** and are **Docker-gated**
(`@Testcontainers(disabledWithoutDocker = true)`) — they self-skip when no Docker
daemon is reachable, and run for real when Docker/Colima is up (`Skipped: 0` then).
Ryuk is disabled in the Surefire config (it can't bind-mount Colima's socket).

Two suites are load-bearing for the ongoing refactor and must stay green:

- **The characterization suites** — `CustomerOrderServiceTest`, `CartServiceTest`,
  `ReviewServiceTest`. These lock the *current* behavior of the three services S10–S12
  rewrite, **bugs included**; the pinned bugs are labelled `KNOWN BUG` with the sprint
  expected to change them. A deliberate behavior change means editing the assertion **in
  the same commit**, with the rationale in the commit message.
- **`ResponseDtoWireCompatTest`** — pins the JSON wire format byte-for-byte against the
  document shape. Both frontends depend on it.

Testcontainers' `MongoDBContainer` always starts a single-node replica set, so the
container-backed tests get real transactions for free — no extra setup beyond Docker.

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
