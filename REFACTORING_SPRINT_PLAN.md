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
- [ ] **Baseline:** confirm `mvn -q compile` green; add empty defaults for the two undefined `github.models.token` / `github.models.secondToken` props so context loads.
- [ ] **Housekeeping:** relocate `backend/src/main/java/com/dominator/bookify/data/*.json` (incl. the 37 MB `cities.json`) → `backend/data/seed/`; add to `.gitignore` if not needed in-repo. Delete dead `controller/admin/template.java`.
- [ ] **Externalize secrets:** replace literals in `application.properties` + `application-docker.properties` with env placeholders (`${JWT_SECRET}`, `${MAIL_PASSWORD}`, `${MOMO_ACCESS_KEY}`, `${MOMO_SECRET_KEY}`, `${GOOGLE_CLIENT_ID}`, `${GITHUB_MODELS_TOKEN}`, …). Add `backend/.env.example` + README section; thread vars through `docker-compose*.yml` / `Dockerfile`.
- [ ] **Config fixes:** `http://localhost:27017` → `mongodb://…`; consolidate CORS (one `cors.allowed-origins` property + one `CorsConfigurationSource`) removing duplication across `WebConfig`, `WebSocketCorsConfig`, `websocket/WebSocketConfig`; migrate `SecurityConfig` off deprecated `.cors().and()`.
- [ ] **Lock admin surface:** remove `/api/admin/**` (and redundant siblings) from `permitAll()`; add `.requestMatchers("/api/admin/**").hasRole("ADMIN")`. Keep genuinely public routes public (auth, catalog reads, webhooks, `/ws-chat/**`).
- [ ] **Wire authorities:** ensure `JwtAuthenticationFilter` maps `User.role` → `ROLE_ADMIN`/`ROLE_CUSTOMER`.
- [ ] **Out-of-band:** rotate the now-exposed secrets (they remain in git history).

**Verify:** `/api/admin/orders` → 401/403 without an admin token, 200 with one; app boots only when env vars are set; no `http://…27017` remains.

**Risks:** locking admin may break the admin frontend if it doesn't send the JWT → confirm/patch FE auth header (small companion change). Missing env var → boot fails loudly (intended).

---

## Sprint 2 — Uniform Error Handling + Web Layer
**Goal:** One global exception handler replaces ~30 copy-pasted try/catch blocks; controllers become thin and return validated DTOs, never entities. *(Phases 3–4)*

**Backlog**
- [ ] **`exception/` package:** `ApiException(HttpStatus,msg)` + `ResourceNotFoundException`, `BadRequestException`, `UnauthorizedException`, `ConflictException`; `ErrorResponse` record; `GlobalExceptionHandler` (`@RestControllerAdvice`) handling `ApiException`, `ResponseStatusException`, `MethodArgumentNotValidException` (field errors), and a catch-all 500.
- [ ] **Strip inline try/catch** from every controller (`AdminOrderController` ~13×, `CartController` 8×, `OrderController`, `UserController`, `BookController`, …); replace service-thrown `RuntimeException` (13× in `UserService`) and ad-hoc `ResponseStatusException` with the typed exceptions.
- [ ] **DTO-only responses:** add response DTOs where controllers return `Order`/`Cart`/`Book`/`CartItem`; standardize handlers to `ResponseEntity<TypedDto>`.
- [ ] **Validation:** replace raw `Map<String,String>` bodies (`UserController.resend/forgotPassword/changePassword`, `AdminOrderController` binding raw `Order`) with request DTOs; apply `@Valid` uniformly; add bean-validation annotations to DTOs missing them.
- [ ] **De-leak controllers:** move Google-token verify + user upsert from `OAuthController` into an `OAuthService`; route `CartController.deleteGuestCart` through `CartService`.
- [ ] **Collapse dashboard:** merge `AdminDashboardController` interface + `AdminDashboardControllerImplement` into one idiomatic controller returning `ResponseEntity`.
- [ ] **Standardize:** `@RequiredArgsConstructor`; `@AuthenticationPrincipal AuthenticatedUser authUser` (single name); route pluralization (`/api/cart`→`/api/carts`, `/api/wishlist`→`/api/wishlists`, `/api/blogposts`→`/api/blog-posts`) — **note FE companion changes for S5**.
- [ ] **Tests:** `@WebMvcTest` for `GlobalExceptionHandler` + one admin & one user controller (400 on bad input, 404 not-found, 403 admin-denied).

**Verify:** bad input → uniform `ErrorResponse` 400 with field errors; missing id → 404; no controller contains a `try/catch` returning `Map.of("error", …)`.

**Risks:** route pluralization changes the FE contract — either defer the path renames to S5 (recommended, to batch FE work) or add temporary aliases. Decide at sprint start.

---

## Sprint 3 — Service Layer (SRP + consistent interface/impl)
**Goal:** Break up god-services into single-responsibility collaborators and impose one naming convention. *(Phase 5)*

**Backlog**
- [ ] **One convention:** each service = interface `XxxService` + `impl/XxxServiceImpl` (or drop interfaces uniformly — pick one). Fix `...Imp`→`...Impl`, drop `...Interface` suffix, rename `AdminDashboardControllerImplement`, and resolve the `admin.OrderService` (interface) vs `user.OrderService` (class) collision → `AdminOrderService` / `CustomerOrderService`.
- [ ] **Split `UserService`** (249 lines) → `UserService` (profile/account), `AuthService` (login/JWT/password), `VerificationTokenService` (token lifecycle + email trigger), `AvatarStorageService` (file IO → move writes from `frontend/public/...` to configured `uploads/`).
- [ ] **Split admin `OrderServiceImpl`** (314 lines) → extract Mongo analytics to `OrderAnalyticsService`; collapse the 7 duplicate status-transition methods into one `transition(id, targetStatus)` guarded by an allowed-transition map; rename `test()` → `buildPayment(...)`, move payment/transaction assembly into a `PaymentFactory`/`TransactionService`.
- [ ] **Shorten long methods:** `CustomerOrderService.createOrder`, `ReviewService.createReview`, `GithubModelsService.getAIResponse` → private step methods; remove `printStackTrace`.
- [ ] **DI hygiene:** remove redundant `@Autowired` on `final` fields (dashboard trio); give `AdminReviewService`/`GithubModelsService` `@RequiredArgsConstructor`.
- [ ] **Transactions:** add `@Transactional` to admin order status transitions and `UserService.register`.
- [ ] **Repackage:** move `AddressService` → `service/common/`; keep AI under its own vertical.
- [ ] **Tests:** unit tests for `AuthService` (login/register/verify/reset), `OrderAnalyticsService`/transition map.

**Verify:** each service class has a single clear responsibility; no method > ~40 lines in the touched files; auth + order flows still behave via smoke test.

**Risks:** highest logic-churn sprint — lean on the S2 controller tests + new unit tests as the safety net. Split incrementally (one god-service at a time, compile between).

---

## Sprint 4 — Mapper Layer + Repository/AI Tidy
**Goal:** Centralize entity↔DTO mapping and clean up remaining repository/AI rough edges. *(Phases 6–7)*

**Backlog**
- [ ] **`mapper/` package:** plain `@Component` mappers (`UserMapper`, `ProductMapper`*, `OrderMapper`, `ReviewMapper`, `CartMapper`, …) with `toDto`/`toEntity`/`updateEntity`. *(named `Book…` until S5)*
- [ ] **Remove inline mapping** from services/controllers (`UserService.convertToUserDTO`, `CartService.getCartItem`, `OrderService.buildOrderItem`, `ReviewService`, `BookService.getBooksByIds`, dashboard DTO builds, `BeanUtils.copyProperties`). Aggregation→DTO Mongo projections stay in repositories.
- [ ] **Repo naming:** `OrderRepositoryImpl` → `OrderRepositoryCustomImpl` (match `BookRepositoryCustomImpl`); standardize injection.
- [ ] **AI tidy:** replace intent magic strings with the existing `NavigationTarget`/intent enum; extract duplicated raw-`HttpClient` GitHub-Models logic into one `GithubModelsClient`; externalize hardcoded prompts; drop `ChatController`'s unused `GithubModelsService` dep; translate Vietnamese comments.
- [ ] **Model consistency:** unify audit timestamps on `Instant` across `Book/Order/User`; fix `idx_items_bookdId` typo.
- [ ] **Tests:** mapper unit tests (round-trip a couple of entities).

**Verify:** grep shows no `new …DTO(` / `copyProperties` mapping left in services; AI intents reference enum constants; app boots.

**Risks:** low — mostly mechanical extraction. Changing timestamp types touches serialization; verify JSON output shape unchanged for FE.

---

## Sprint 5 — The Great Rename → Gearly / Product  ⚠️ highest blast radius
**Goal:** One mechanical sweep renaming Bookify→Gearly and Book→Product across backend, DB, and both frontends. *(Phase 8)* **Do a full DB + repo snapshot first.**

**Backlog**
- [ ] **Package:** `com.dominator.bookify` → `com.dominator.gearly` (move tree, fix all `package`/`import`; update `pom.xml` groupId/artifactId/name; `spring.application.name`). `BookifyApplication` → `GearlyApplication` (+ test).
- [ ] **Domain:** `Book`→`Product`, `BookRepository`→`ProductRepository`, `BookService`→`ProductService`, `Book*DTO`→`Product*DTO`, routes `/api/books`→`/api/products`, `@Document(collection="books")`→`"products"`, all identifiers (51 backend files reference `Book`).
- [ ] **Apply deferred route renames** from S2 here to batch FE work.
- [ ] **DB migration:** rename Mongo collections (`books`→`products`, …); re-seed from relocated dumps; commit a repeatable migration script. **Mutates the real DB — snapshot taken.**
- [ ] **Frontends (companion):** update API paths + `book`→`product` in `frontend/src` and `frontend_admin/bookify/src` (~21 files); rename the `frontend_admin/bookify/` directory.
- [ ] **Admin FE real auth (deferred from S1):** the admin app currently uses *fake* demo auth — `authProvider.login` stores `` `${email}-${password}` `` in `localStorage` (never calls the backend) and the stock `@refinedev/simple-rest` data provider sends **no `Authorization` header**. Since S1 locked `/api/admin/**` behind `ROLE_ADMIN`, the admin app now 403s on every call. Wire a real login against `/api/users/login`, persist the returned JWT, and attach `Authorization: Bearer <token>` to all `/api/admin/**` requests (custom data provider / fetch wrapper). Also review `useAutoLoginForDemo`.
- [ ] **OpenAPI:** add `springdoc-openapi-starter-webmvc-ui` so the renamed surface is browsable/verifiable.

**Verify:** `grep -ri "bookify" backend/src` and `grep -ri "\bBook\b" backend/src/main/java` return only intentional matches; `/api/products` responds; Swagger UI lists the new surface; both frontends load and browse catalog.

**Risks:** **highest.** Isolate to its own branch; do backend rename → compile green → DB migrate → FE update, in that order. Keep the snapshot to roll back. Consider IDE-assisted "Rename" refactors for the domain symbols to avoid missed references.

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
