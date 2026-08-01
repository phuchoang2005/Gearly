# Gearly Backend — Refactoring Sprint Plan

> **Format:** Solo-developer sprints. One-week cadence (≈4 focused dev-days/sprint, leaving buffer for review, bugfix, and life). Each sprint ends on a **green build + a tag**, so any sprint is a safe stopping point. Work one sprint branch at a time; commit per checklist item so regressions are bisectable.

---

## Why this refactor

The backend (`/backend`, Spring Boot 3.4.3 / Java 21 / MongoDB) is named **Bookify** (`com.dominator.bookify`) but the domain is a **PC/computer-component store** — its real identity is **Gearly**. A survey of 151 Java files (with ~0 real tests) found systemic issues: no global error handling (the same try/catch is copy-pasted 13× in one controller), god-services mixing DB + JWT + file IO + email + payments, chaotic interface/impl naming, an `/api/admin/**` surface left publicly accessible, secrets committed in `application.properties`, controllers returning entities, and inline DTO mapping everywhere.

**Confirmed direction:** full rename Bookify→Gearly incl. domain `Book`→`Product` (migrating DB + both frontends), fix both security issues, hand-written `@Component` mappers, and a safety-net test baseline.

---

## Sprint map at a glance

| Sprint | Theme | Phases | Est. | Risk | Ships |
|--------|-------|--------|------|------|-------|
| **S1** | Foundation, Config & Security | 0, 1, 2 | ~3.5 d | Med | Secure, boots on env vars, dumps out of src |
| **S2** | Uniform Error Handling + Web Layer | 3, 4 | ~4.5 d | Med | Global handler, DTO-only controllers, validation |
| **S3** | Service Layer (SRP + naming) | 5 | ~4 d | High | Split god-services, consistent interface/impl |
| **S4** | Mapper Layer + Repo/AI Tidy | 6, 7 | ~4 d | Low | `mapper/` package, tidy repos & AI clients |
| **S5** | The Great Rename → Gearly/Product | 8 | ~4 d | **Highest** | Renamed package+domain, DB + FE migrated |
| **S6** | Test Hardening & Stabilization | 9 | ~3 d | Low | Green `mvn test`, OpenAPI docs, regression pass |

**Dependency order is strict:** S1 → S2 → S3 → S4 → S5 → S6. S5 is done *after* the structure is clean so it's a single mechanical sweep. Testing is written *within* each sprint for what that sprint touched; **S6 consolidates** and fills gaps.

---

## Cross-cutting working agreements (solo-dev)

- **Definition of Ready (per item):** the file(s) are identified, the desired end-state is clear, and there's a way to verify it (compile, boot, curl, or test).
- **Definition of Done (per sprint):**
  1. `cd backend && mvn -q compile` is green (and `mvn test` green from S2 onward).
  2. App boots (`mvn spring-boot:run` with env vars) — no missing-bean/placeholder errors.
  3. The sprint's behavior is manually smoke-verified (see each sprint's *Verify*).
  4. New/changed code follows the target conventions (below).
  5. Sprint branch merged to `main`, tagged `refactor/sN-<theme>`.
- **Branch/commit:** one branch per sprint (`refactor/s1-foundation`, …). Commit per checklist box with a clear message. No PR ceremony; do a **self-review diff pass** before merging.
- **Target conventions** (apply as you touch code): `@RequiredArgsConstructor` + `final` fields; constructor injection only (no field `@Autowired`); controllers return `ResponseEntity<TypedDto>` (never entities); services throw typed exceptions; one class = one responsibility; English comments.
- **Safety:** snapshot MongoDB before S5. Keep the relocated seed data to restore from.

---

## Sprint 1 — Foundation, Config & Security
**Goal:** Establish a safe, buildable baseline; get secrets out of source; make admin endpoints actually require an admin. *(Phases 0–2)*

**Backlog**
- [x] **Baseline:** confirm `mvn -q compile` green; add empty defaults for the two undefined `github.models.token` / `github.models.secondToken` props so context loads.
- [x] **Housekeeping:** relocate `backend/src/main/java/com/dominator/bookify/data/*.json` (incl. the 37 MB `cities.json`) → `backend/data/seed/`; add to `.gitignore` if not needed in-repo. Delete dead `controller/admin/template.java`.
- [x] **Externalize secrets:** replace literals in `application.properties` + `application-docker.properties` with env placeholders (`${JWT_SECRET}`, `${MAIL_PASSWORD}`, `${MOMO_ACCESS_KEY}`, `${MOMO_SECRET_KEY}`, `${GOOGLE_CLIENT_ID}`, `${GITHUB_MODELS_TOKEN}`, …). Add `backend/.env.example` + README section; thread vars through `docker-compose*.yml` / `Dockerfile`.
- [x] **Config fixes:** `http://localhost:27017` → `mongodb://…`; consolidate CORS (one `cors.allowed-origins` property + one `CorsConfigurationSource`) removing duplication across `WebConfig`, `WebSocketCorsConfig`, `websocket/WebSocketConfig`; migrate `SecurityConfig` off deprecated `.cors().and()`.
- [x] **Lock admin surface:** remove `/api/admin/**` (and redundant siblings) from `permitAll()`; add `.requestMatchers("/api/admin/**").hasRole("ADMIN")`. Keep genuinely public routes public (auth, catalog reads, webhooks, `/ws-chat/**`).
- [x] **Wire authorities:** ensure `JwtAuthenticationFilter` maps `User.role` → `ROLE_ADMIN`/`ROLE_CUSTOMER`.
- [ ] **Out-of-band:** rotate the now-exposed secrets (they remain in git history). *(User action — cannot be done in-repo.)*

> **S1 status:** ✅ complete. Merged to `main`, tagged `refactor/s1-foundation`. Pre-refactor baseline snapshotted on branch `old-project`.

**Verify:** `/api/admin/orders` → 401/403 without an admin token, 200 with one; app boots only when env vars are set; no `http://…27017` remains.

**Risks:** locking admin may break the admin frontend if it doesn't send the JWT → confirm/patch FE auth header (small companion change). Missing env var → boot fails loudly (intended).

---

## Sprint 2 — Uniform Error Handling + Web Layer
**Goal:** One global exception handler replaces ~30 copy-pasted try/catch blocks; controllers become thin and return validated DTOs, never entities. *(Phases 3–4)*

**Backlog**
- [x] **`exception/` package:** `ApiException(HttpStatus,msg)` + `ResourceNotFoundException`, `BadRequestException`, `UnauthorizedException`, `ConflictException`; `ErrorResponse` record; `GlobalExceptionHandler` (`@RestControllerAdvice`) handling `ApiException`, `ResponseStatusException`, `MethodArgumentNotValidException` (field errors), `HttpMessageNotReadable` (400), and a catch-all 500. *(`ErrorResponse.error` intentionally carries the message key both frontends read.)*
- [x] **Strip inline try/catch** from every controller (`AdminOrderController` ~13×, `CartController` 8×, `OrderController`, `UserController`, `BookController`, …); replace service-thrown `RuntimeException` (13× in `UserService`) and ad-hoc `ResponseStatusException` (admin + user services) with the typed exceptions. *(Only 2 `catch` blocks remain, both intentional: `UserController.verifyToken` browser redirect, `PaymentController.hmacSha256` crypto helper.)*
- [x] **DTO-only responses:** typed `ResponseEntity<T>` everywhere (no `ResponseEntity<?>` left); `MessageResponse` for ack bodies. *(Deferred to S4 w/ mappers: `Order`/`Cart`/`Book`/`Review` **entity** responses → response DTOs — marked with `TODO(S4)`.)*
- [x] **Validation:** raw `Map<String,String>` bodies replaced with `EmailRequestDTO` / `ChangePasswordRequestDTO`; `@Valid` applied on create/update bodies. *(Deferred to S3: `AdminOrderController` raw `@RequestBody Order` → request DTO, tied to the admin-order-service restructure — `TODO(S3)`.)*
- [x] **De-leak controllers:** Google-token verify + user upsert moved from `OAuthController` into `OAuthService`; `CartController.deleteGuestCart` routed through `CartService` (no more direct `CartRepository`).
- [x] **Collapse dashboard:** merged `AdminDashboardController` interface + `AdminDashboardControllerImplement` into one idiomatic controller returning `ResponseEntity`.
- [x] **Standardize:** `@RequiredArgsConstructor`; single principal name `authUser`. *(Route pluralization **deferred to S5** to batch FE changes — user decision.)*
- [x] **Tests:** `@WebMvcTest` — `UserControllerTest` (400 field errors / 401 / 404 via handler) + `AdminBookControllerTest` (404 + admin-denied 403). `mvn test` green (10 run, 1 skipped: `contextLoads`, re-enabled in S6).

**Verify:** bad input → uniform `ErrorResponse` 400 with field errors; missing id → 404; no controller contains a `try/catch` returning `Map.of("error", …)`.

**Risks:** route pluralization changes the FE contract — either defer the path renames to S5 (recommended, to batch FE work) or add temporary aliases. Decide at sprint start.

---

## Sprint 3 — Service Layer (SRP + consistent interface/impl)
**Goal:** Break up god-services into single-responsibility collaborators and impose one naming convention. *(Phase 5)*

**Backlog**
- [x] **One convention:** **dropped interfaces uniformly** (user decision) — every service is now a plain `@Service` class. Deleted the admin service interfaces and folded each single impl into the clean name; fixed `...Imp`/`...Impl` suffixes (`AdminDashboardServiceImp`→`AdminDashboardService`, `…GetBookImp`→`AdminDashboardGetBookService`, `…GetUserImp`→`AdminDashboardGetUserService`); resolved the `admin.OrderService` vs `user.OrderService` collision → `AdminOrderService` / `CustomerOrderService`. *(`AdminDashboardControllerImplement` was already collapsed in S2.)*
- [x] **Split `UserService`** → `UserService` (profile/account), `AuthService` (login/JWT/password), `VerificationTokenService` (token lifecycle + email trigger), `AvatarStorageService` (file IO). Avatar writes now go to a configured `uploads/avatars` dir (served at `/uploads/**`) via `app.avatar.*` props. *(convertToUserDTO duplicated in UserService/AuthService — `TODO(S4)` to fold into `UserMapper`.)*
- [x] **Split admin `OrderServiceImpl`** → analytics extracted to `OrderAnalyticsService`; the 7 duplicate status-transition methods collapsed into one `transition(id, targetStatus)` guarded by an allowed-source map (+ a small tx-effect map for money-affecting transitions); the misnamed `test()` and inline payment builds replaced by `PaymentFactory` (in `service/common/`).
- [x] **Shorten long methods:** `CustomerOrderService.createOrder`, `ReviewService.createReview`, `GithubModelsService.getAIResponse` → private step methods; `printStackTrace` removed (AI service now uses `@Slf4j`).
- [x] **DI hygiene:** dropped redundant `@Autowired` on `final` fields (dashboard trio); gave `AdminReviewService`/`GithubModelsService` `@RequiredArgsConstructor`.
- [x] **Transactions:** `@Transactional` on `AdminOrderService.transition` and `AuthService.register`.
- [x] **Repackage:** `AddressService` → `service/common/` (joined by `PaymentFactory`); AI kept in its own vertical.
- [x] **Tests:** `AuthServiceTest` (login/register/reset/change-password) + `AdminOrderServiceTest` (transition map). `mvn test` green (22 run, 1 skipped). *(`OrderAnalyticsService` aggregation coverage left to S6 integration tests — needs Mongo.)*

> **S3 status:** ✅ complete on branch `refactor/s3-service-layer` (not yet merged/tagged — commits only, per request). Build + `mvn test` green (22 run, 1 skipped). **Deferred to S5** (batched with the admin-FE rework): admin `@RequestBody Order` → request DTO on create/update; and the avatar public-URL switch to `/uploads/...` (FE must read it from the backend origin).

**Verify:** each service class has a single clear responsibility; no method > ~40 lines in the touched files; auth + order flows still behave via smoke test.

**Risks:** highest logic-churn sprint — lean on the S2 controller tests + new unit tests as the safety net. Split incrementally (one god-service at a time, compile between).

---

## Sprint 4 — Mapper Layer + Repository/AI Tidy
**Goal:** Centralize entity↔DTO mapping and clean up remaining repository/AI rough edges. *(Phases 6–7)*

**Backlog**
- [x] **`mapper/` package:** plain `@Component` mappers (`UserMapper`, `BookMapper`*, `OrderMapper`, `ReviewMapper`, `CartMapper`, `BlogPostMapper`, `StaticPageMapper`) with `toDto`/`toEntity`/`updateEntity`. Category-name and display-name lookups stay in services and are passed in. *(named `Book…` until S5)*
- [x] **Remove inline mapping** from services/controllers (`UserService`/`AuthService`/`OAuthService.convertToUserDTO`, `CartService.getCartItem`, `CustomerOrderService.buildOrderItem`, `ReviewService`/`AdminReviewService`, `BookService.getBooksByIds`/`WishlistService`, dashboard low-stock build, `AdminBookService`/`AdminUserService` `BeanUtils.copyProperties`). Aggregation→DTO Mongo projections stay in the dashboard services. *(Two intentional leftovers: `AdminOrderService`'s entity←`@RequestBody Order` `copyProperties` — tied to the admin request-DTO work **deferred to S5**; and `AddressController`'s trivial `AddressOptionDTO` projection — out of scope.)*
- [x] **Repo naming:** `OrderRepositoryImpl` → `OrderRepositoryCustomImpl` (match `BookRepositoryCustomImpl`); both custom impls standardized on `@RequiredArgsConstructor`.
- [x] **AI tidy:** added `Intent` enum (replaces free-form intent strings; `AiDecision` carries it, `AiRouter` switches on it); extracted the duplicated raw-`HttpClient` GitHub-Models logic into one `GithubModelsClient`; externalized the three prompts to `classpath:prompts/*.txt` (loaded by `AiPrompts`); dropped `ChatController`'s unused `GithubModelsService` dep; translated the Vietnamese dashboard-aggregation comments.
- [x] **Model consistency (partial):** `User.createdAt/updatedAt` (+ `AdminUserDTO`) `LocalDateTime` → `Instant` (Order already `Instant`). **Deferred to S5:** `Book.addedAt/modifiedAt` `String` → `Instant` and the `idx_items_bookdId` → `idx_items_bookId` rename — both need coordinated DB changes (mixed String/Date data would corrupt the "newest" sort; the index rename conflicts under `auto-index-creation=true` against a DB holding the old index) that the S5 migration + re-seed does atomically.
- [x] **Tests:** `UserMapperTest` + `BookMapperTest` (round-trip, image re-wrap, rating/id preservation, String→ObjectId category conversion). `mvn test` green (27 run, 1 skipped).

> **S4 status:** ✅ complete on branch `refactor/s4-mappers` (commits only, not merged/tagged — per request). Build + `mvn test` green (27 run, 1 skipped: `contextLoads`). **Deferred to S5:** Book timestamps → `Instant`; `idx_items_bookdId` index rename; admin `@RequestBody Order` → request DTO (already an S5 item). The S2-deferred "raw entity responses → response DTOs" (`Order`/`Cart`/`Book`/`Review`) remains open — not part of the S4 checkboxes and FE-contract-adjacent, so it stays batched with S5/S6. `BookController.getBook` still returns a raw `Book`.

**Verify:** grep shows no entity↔DTO `new …DTO(` / `copyProperties` mapping left in services (only the deferred `AdminOrderService` entity copy remains); AI intents reference `Intent`/`NavigationTarget` enum constants; compiles green.

**Risks:** low — mostly mechanical extraction. Timestamp-type change touches serialization: `User` audit fields now serialize as UTC ISO-8601 (`…Z`) instead of a zoneless local ISO string — verify the admin FE's date rendering.

---

## Sprint 5 — The Great Rename → Gearly / Product  ⚠️ highest blast radius
**Goal:** One mechanical sweep renaming Bookify→Gearly and Book→Product across backend, DB, and both frontends. *(Phase 8)* **Do a full DB + repo snapshot first.**

**Backlog**
- [x] **Package:** `com.dominator.bookify` → `com.dominator.gearly` (whole main+test tree moved, all `package`/`import` fixed; `pom.xml` artifactId/name/description + `spring.application.name` → Gearly; `BookifyApplication` → `GearlyApplication` + test). Compile + `mvn test` green (27 run, 1 skipped).
- [x] **Domain:** `Book`→`Product` everywhere — entity (`@Document "products"`), repositories/services/mappers/controllers, all `Book*DTO`, admin `Book*` controllers/services, every identifier (`getBook`→`getProduct`, `bookId`→`productId`, …); routes `/api/books`→`/api/products`, `/api/admin/books`→`/api/admin/products`.
- [x] **Apply deferred route renames** from S2. *(User decision: **leave singular** — `/api/cart`, `/api/guest-cart`, `/api/wishlist` are singleton-per-user resources; no route/FE change. Item is a no-op.)*
- [x] **DB migration:** database renamed `bookify` → `gearly` (all mongo URIs), collection `books` → `products`, and nested `bookId` → `productId` (carts/reviews/blogPosts). `data/seed/seed.sh` re-seeds the transformed dumps; `data/seed/migrate.js` does an idempotent in-place migration for a live DB. **Verified on a Dockerized (Colima) mongo:** fresh seed → `products=51`, `productId` everywhere, 0 residual `bookId`; migrate.js converts a Bookify-shaped DB and is idempotent. *(Not run against any real DB — Mongo wasn't running here; scripts are for the user to run after their snapshot.)*
- [x] **Model consistency (from S4) — index only:** `Order` index typo `idx_items_bookdId` → `idx_items_productId` (renamed with the domain, better than the planned `…bookId`). *(**Deferred to S6:** `Product.addedAt/modifiedAt` `String` → `Instant` + the corresponding data conversion — timestamps are uniformly `String` today, so there's no mixed-window risk; batch the type change with its data migration + admin-FE date-render check in the buffer sprint.)*
- [x] **Frontends (companion):** `book`→`product` + `/api/books`→`/api/products` + Bookify→Gearly across `frontend/src` (storefront, **`vite build` green**) and `frontend_admin/` (admin). Renamed the admin directory `frontend_admin/bookify/` → `frontend_admin/gearly/`. *(Admin was a half-migrated finefoods template: per user decision, deleted the orphan template `product` implementation — `pages/products` + `components/product` + the sidebar-less `/products` route — then renamed the real Book domain to Product, resource `books`→`products`, i18n keys included. Removed a dead unimported placeholder `order/orderDrawerForm`. Rename adds **zero** new type errors — tsc 122 vs HEAD baseline 129; remaining are pre-existing template debt, out of scope.)*
- [x] **Admin FE real auth (deferred from S1):** `authProvider.login` now POSTs `/api/users/login`, persists the JWT, and probes `/api/admin/users` to confirm `ROLE_ADMIN` before admitting the user; an axios interceptor attaches `Authorization: Bearer <token>` to all `/api/admin/**` calls via the data provider; `onError` logs out on 401/403; `useAutoLoginForDemo` neutered (no more demo login). **Verified end-to-end against the backend:** admin login → token → `/api/admin/users` 200; customer token → 403; no token → 403; bad password → 401.
- [x] **OpenAPI:** added `springdoc-openapi-starter-webmvc-ui` + a branded `OpenApiConfig` (“Gearly API”); permitted `/v3/api-docs/**`, `/swagger-ui/**` in `SecurityConfig`. Verified: `/v3/api-docs` 200 (82 paths incl. `/api/products*`), `/swagger-ui/index.html` 200.
- [x] **Docker setup (user request):** added `backend/Makefile` (up/dev/down/clean/build/logs/ps + mongo-up/seed/migrate/mongosh + local mvn compile/test/run pinned to JDK 21); dropped the obsolete compose `version:` key. Verified `make seed` / `make migrate` against the Dockerized mongo.

**Verify:** `grep -ri "bookify" backend/src` and `grep -ri "\bBook\b" backend/src/main/java` return only intentional matches; `/api/products` responds; Swagger UI lists the new surface; both frontends load and browse catalog.

**Risks:** **highest.** Isolate to its own branch; do backend rename → compile green → DB migrate → FE update, in that order. Keep the snapshot to roll back. Consider IDE-assisted "Rename" refactors for the domain symbols to avoid missed references.

> **S5 status:** ✅ complete on branch `refactor/s5-rename` (commits only, not merged/tagged — per request; not pushed). Backend compile + `mvn test` green (27 run, 1 skipped); storefront `vite build` green; admin type-clean re: the rename (no new tsc errors vs baseline). DB migration + auth flow + OpenAPI verified against a live Dockerized (Colima) stack. **Deferred to S6:** `Product.addedAt/modifiedAt` `String`→`Instant` (+ data migration + admin date-render check); the pre-existing finefoods-template admin tsc debt (122 errors, unrelated to the rename); and the still-open S2/S4 items (admin `@RequestBody Order`→request DTO; avatar public-URL `/uploads/...`; raw entity responses `Order`/`Cart`/`Product`/`Review`→response DTOs). **User action:** run `data/seed/migrate.js` (or `make -C backend seed`) against the real DB after snapshotting.

---

## Sprint 6 — Test Hardening & Stabilization
**Goal:** Turn the per-sprint tests into a coherent safety-net suite; final regression pass. *(Phase 9)*

**Backlog**
- [ ] **Fill gaps:** unit tests for `UserService`/`AuthService`, `OrderAnalyticsService`, mappers (`ProductMapper`, `OrderMapper`, `UserMapper`).
- [ ] **Slice tests:** `@WebMvcTest` covering `GlobalExceptionHandler` + representative admin/user controllers (validation, not-found, admin-denied).
- [ ] **Context:** `GearlyApplicationTests.contextLoads` green with test properties (`@DataMongoTest`/embedded or mocked Mongo where needed).
- [ ] **Regression:** end-to-end run of both frontends against the backend — catalog browse → cart → order → review — confirming rename + refactor preserved behavior.
- [ ] **Docs:** finalize README (env vars, run instructions, architecture overview), confirm Swagger UI.

**Verify:** `mvn test` green; e2e happy-path passes; `README` lets a fresh clone build + run.

**Risks:** low. This is the buffer sprint — absorb any spillover from S3/S5 here.

---

## Solo-dev tips for running this

- **Timebox, don't perfect.** If a sprint item balloons, cut scope to the DoD and log a follow-up rather than slipping the tag.
- **Green between every service split** in S3 — never leave the tree uncompilable overnight.
- **S5 is the one to fear.** Snapshot, branch, and rename with IDE tooling; verify FE before merging.
- **Keep the plan honest:** check boxes as you go; move anything unfinished to S6's buffer.
