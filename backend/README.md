# Gearly — Backend

REST + realtime API for **Gearly**, a PC / computer-component store: catalog,
cart, checkout, orders, reviews, blog/static pages, an admin console, an AI
shopping assistant, and MoMo payments.

**Stack:** Spring Boot 3.4.3 · Java 21 · MongoDB · Spring Security (JWT) ·
WebSocket · springdoc/OpenAPI · langchain4j (GitHub Models).

---

## Architecture

The backend is organised as **bounded contexts with rich aggregates**, the result of
Sprints 8–13 of [`DDD_REFACTORING_PLAN.md`](../DDD_REFACTORING_PLAN.md). There is no
`controller/`, `service/`, `model/` or `repository/` package: every class belongs to a
context, and the layer rules are enforced by `ArchitectureFitnessTest` rather than by
convention.

### The contexts

| Context | Role | What it owns |
|---|---|---|
| `ordering/` | **Core** | The `Order` aggregate and its state machine, `PricingPolicy`, the payment ledger. An order cannot be put into an illegal state. |
| `catalog/` | **Core** | `Product`, `Category`, the one stock rule, and the `CatalogSnapshot` ACL other contexts capture prices through. |
| `cart/` | Supporting | The `Cart` aggregate: add, merge, reconcile with the catalog. A line is constructible only from a snapshot. |
| `reviews/` | Supporting | The review lifecycle and the `ReviewApproved` / `ReviewRejected` events that drive a product's rating. |
| `identity/` | Supporting | The `User` aggregate, verification tokens, the access boundary. |
| `content/` | Supporting | Blog posts and standing pages. Read-only. |
| `payments/` | Generic (ACL) | `PaymentGateway` and `ExchangeRateProvider` ports; the MoMo and FX adapters. Nothing else knows a provider exists. |
| `notification/` | Generic | `NotificationSender` port, SMTP adapter, message catalogue and layout. |
| `storage/` | Generic | `FileStorage` port and the local-disk adapter, with content-type and size validation. |
| `geo/` | Generic | The country/state/city reference dataset behind `PlaceDirectory`. |
| `assistant/` | Generic | The chat assistant behind `AiAssistant`; the language model is an adapter. |
| `analytics/` | Query side | Dashboard and sales read models. Reads documents, returns DTOs, never names an aggregate. |

Two packages are deliberately **not** contexts:

| Package | Role |
|---|---|
| `shared/domain/` | The shared kernel: `Money`, `Quantity`, `Rating`, `EmailAddress`, typed ids, `AggregateRoot`, `DomainEvent`, and the five domain-exception bases. Depends on no context. |
| `shared/api/` | The handful of response shapes more than one context answers with. Kept very small. |
| `platform/` | Cross-cutting wiring: `config`, `security`, `exception`. Knows about the contexts; they do not know about it. |

### The layers inside a context

| Package | Responsibility |
|---|---|
| `<context>/domain/` | Aggregates, value objects, domain services, events and the repository/external-system **ports**. Plain Java — no web, security, HTTP or Spring Data repository types, so every one of them is constructible in a test with no Spring context. |
| `<context>/application/` | Use cases. Take a command record plus a typed id, never a Spring Security principal. One aggregate per transaction. |
| `<context>/infrastructure/` | Adapters implementing the domain's ports. The only layer that may name `MongoTemplate` or a Spring Data repository. |
| `<context>/api/` | Controllers and their DTOs. Unwraps the authenticated principal into a typed id before calling in. |

Not every context has all four. `payments/`, `notification/` and `storage/` are generic
subdomains with a `domain` (the port) and an `infrastructure` (the adapter) and no inbound
HTTP edge of their own — the MoMo callback is an `ordering/` endpoint, because its effect is
to move an order.

### How contexts reach each other

Only through another context's **published language**: a port (an interface in its `domain`
package), a domain event, or a published value such as a `*Snapshot` or a typed `*Id`.
An application service, a repository adapter or a controller of another context is never a
legal target — that is the distributed-monolith failure mode the structure exists to prevent.

```
catalog  --CatalogSnapshot / ProductSearchPort-->  ordering, cart, assistant
cart     --OrderPlaced-------------------------->  ordering
ordering --OrderPlaced / OrderCancelled--------->  catalog, cart
ordering --PaymentGateway----------------------->  payments
reviews  --ReviewApproved / ReviewRejected------>  catalog
identity --UserRegistered----------------------->  notification
identity --PlaceDirectory / FileStorage--------->  geo, storage
```

### Error handling

Contexts throw **named domain exceptions** extending one of the shared kernel's five bases;
`platform/exception/GlobalExceptionHandler` maps each base to a status and renders a uniform
`{ timestamp, status, error, fieldErrors? }` body. The domain never names
`org.springframework.http`, so the rule and the status code stay separate concerns.

| Base (in `shared/domain/`) | Status |
|---|---|
| `DomainRuleViolationException` | 400 |
| `AuthenticationFailedException` | 401 |
| `AccessDeniedDomainException` | 403 |
| `DomainNotFoundException` | 404 |
| `DomainConflictException` | 409 |

### The rules are enforced, not documented

`ArchitectureFitnessTest` (ArchUnit) fails the build on: a domain package importing
web/security/HTTP or Spring Data repository types; a `@Document` outside a domain package; a
public setter on an aggregate; `MongoTemplate` outside `analytics` or an adapter; a
`@RequestBody` bound to a stored document; a domain event carrying a publisher-internal type;
an admin route without `@PreAuthorize`; or one context reaching into another other than
through its published language.

**As of S13 every rule applies repo-wide with no scoping.** Sprints 8–12 scoped three of them
to the packages already migrated, because the legacy tree failed them by construction; those
scopes are gone, along with the packages. Each rule has been verified by planting a deliberate
violation and confirming that it — and only it — fails.

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
aggregation test (`SalesAnalyticsQueryIntegrationTest`), the order-repository adapter
test (`MongoOrderRepositoryIntegrationTest`) and the transaction / optimistic-locking
test (`OrderPlacementTransactionIntegrationTest`) spin up a
throwaway MongoDB via **Testcontainers** and are **Docker-gated**
(`@Testcontainers(disabledWithoutDocker = true)`) — they self-skip when no Docker
daemon is reachable, and run for real when Docker/Colima is up (`Skipped: 0` then).
Ryuk is disabled in the Surefire config (it can't bind-mount Colima's socket).

Two suites are load-bearing and must stay green:

- **The characterization suites** — `CartServiceTest`, `ReviewServiceTest`, and the four
  suites S10 split `CustomerOrderServiceTest` into (`PlaceOrderServiceTest`,
  `CancelOrderServiceTest`, `OrderQueryServiceTest`, `OnlinePaymentServiceTest`, plus
  `OrderPlacedListenerTest`). These locked the *current* behavior of the three fat,
  untested services S10–S12 rewrote, **bugs included** — the safety net the whole
  program depended on. A deliberate behavior change means editing the assertion **in
  the same commit**, with the rationale in the commit message; every one that happened
  is listed in the sprint's outcome section.
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
