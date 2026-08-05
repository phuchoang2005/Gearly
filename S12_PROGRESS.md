# S12 in flight — resume notes

> Working doc for the Sprint 12 (Reviews & Identity/Access) run. Delete it once the sprint's
> outcome section lands in `DDD_REFACTORING_PLAN.md`.

**Branch:** `ddd/s12-reviews-identity`, off `main` at `bf8d274`.
**Nothing is pushed and nothing should be** — the instruction for this sprint is commit locally
only.

**⚠️ The branch tip is RED on one ArchUnit rule.** 410 tests run, 409 pass. It is a real finding,
not a flake, and it needs a decision before anything else — see
[The one thing blocking green](#the-one-thing-blocking-green). Do not merge until it is resolved.

## How to build

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # mvn defaults to 26 and breaks Lombok
cd backend && mvn -o test
```

Colima is running and `~/.testcontainers.properties` / `~/.docker-java.properties` are in place,
so the Docker-gated tests (`GearlyApplicationTests`, `DomainTypeBsonRoundTripTest`, the Mongo
integration tests) actually execute rather than self-skipping. `mvnw` is broken (missing
`.mvn/wrapper/`); use system `mvn`. Maven's incremental compile has gone stale twice on this
branch after large file moves — if a deleted class still resolves, `mvn -o clean test-compile`.

## Commits so far

| Commit | What |
|---|---|
| `a276bfb` | `refactor(s12): User becomes the identity aggregate` — green, 383 tests |
| `6eb3523` | `refactor(s12): Review becomes an aggregate with a moderated lifecycle` — **red on one rule**, 410 tests run / 409 pass |

## Backlog state

| # | Backlog item | State |
|---|---|---|
| 4 | `identity/domain` `User` root, TTL to config | **done** (`a276bfb`) |
| 7 | `UserRegistered` + `AFTER_COMMIT` mail listener | **done** (`a276bfb`) |
| 1 | `Review` root with a moderated lifecycle | **done** (`6eb3523`) |
| 3 | Double-review guard + reviewable order status | **done** (`6eb3523`) |
| 2 | Rating rollup driven by `ReviewApproved` | **done in code** (`6eb3523`); the migration has **not been run against a real Mongo** and the seed dumps have **not been re-exported** |
| 5 | Access boundary out of the domain | **mostly done** — services take `UserId`, `AccessDeniedDomainException` is in the shared kernel and `ReviewNotYoursException` uses it, `@EnableMethodSecurity` is on with `@PreAuthorize` on `AdminUserController` and `AdminReviewController`. **Outstanding:** `GlobalExceptionHandler` has **no `@ExceptionHandler` for `AccessDeniedDomainException` yet**, so it currently falls to the catch-all and answers **500 instead of 403**. Fix this first, it is four lines. `@PreAuthorize` is still missing from `AdminOrderController`, `AdminDashboardController`, `MediaController`, `AdminProductController`, `CategoryController`'s admin mapping |
| 6 | The four security holes | **2 of 4 done** — see below |
| 8 | ArchUnit onto `reviews..` / `identity..`, falsified | **partly** — two platform-boundary rules rewritten and one added; the reviews/identity tightening and the falsification pass are outstanding |
| 9 | Tick the backlog, write the outcome section | not started |

### Security items (backlog 6)

- **#2 anonymous `POST /api/reviews/submit-review` → 500 instead of 401** — **done**. The public
  review reads are pinned to `GET` and enumerated in `SecurityConfig`; the write falls through to
  `anyRequest().authenticated()`.
- **#3 JWT filter extract-before-validate → 500 instead of 401** — **done**.
  `AccessTokens.subjectOf` returns an `Optional`, so the wrong ordering cannot be expressed. A
  token for a deleted account takes the same path instead of throwing `UsernameNotFoundException`.
- **#1 IDOR on `GET /api/orders/{id}`** — **outstanding**. `OrderQueryService.findById(String)`
  still has no ownership check and says so in its own javadoc. Give it a `UserId` caller, assert
  `order.isOwnedBy`, raise a new `OrderNotYoursException extends AccessDeniedDomainException`.
  **Verified safe to do:** the admin console's refine dataProvider is based at
  `http://localhost:8080/api/admin` (`frontend_admin/gearly/src/App.tsx:65`), so its `orders`
  resource resolves to `/api/admin/orders/{id}`, which is a separate endpoint. The only caller of
  the customer route is `frontend/src/services/user/orderService.js:10`.
- **#4 unbound guest cart id** — **outstanding**. Plan: `/api/guest-cart/init` issues
  `<uuid>.<hmac>` and every guest-cart call verifies the signature, so the server only ever
  accepts an id it issued — which also stops `getOrCreate` spray-creating carts for arbitrary
  strings. The storefront treats the id as opaque (`useCartData.js` stores whatever `init`
  returned in `localStorage`, `cartService.js` echoes it), so **no frontend change is needed for
  new guests**. A returning guest holding a bare UUID will be refused, so this needs a companion
  storefront change: on that refusal drop `localStorage.guestId` and re-init. Roughly five lines in
  `frontend/src/hooks/user/useCartData.js`.

## The one thing blocking green

```
domain_does_not_depend_on_its_own_infrastructure violated (3 times):
  Field <reviews.domain.Review.productId> has annotation member of type
    <shared.infrastructure.ObjectIdBackedIdConverters$ProductIdAsObjectId>
  … same for orderId and userId
```

`Review` carries `@ValueConverter(ObjectIdBackedIdConverters.…)` on its three id fields, because
a review's `productId`/`orderId`/`userId` are stored as BSON `ObjectId` while the same typed ids
are plain strings on an order line and a cart line. S9 introduced those per-property converters
precisely because `MongoCustomConversions` registers per *type* and cannot express one Java type
with two BSON forms.

The rule never fired before because the class lived in `model/`, which is not a `..domain..`
package. Moving it into the context is what exposed it — the rule is working.

Three ways out, in the order I would consider them:

1. **Normalize the stored form** — make reviews store the three ids as strings like everywhere
   else, drop `@ValueConverter` entirely, and add a `migrate.js` step converting them. S9's own
   note says "S12 owns the reviews context and can decide whether to normalize the stored form",
   so this is the sanctioned option and the only one that removes the asymmetry rather than
   annotating around it. **Cost:** the rating-distribution aggregation in
   `SpringDataReviewRepository` matches `productId` as an `ObjectId` and must change with it; the
   seed dumps must be re-exported; `DomainTypeBsonRoundTripTest`'s
   `reviewIdsStayObjectIdsWhileOrderLineProductIdsStayStrings` and
   `aReviewRemainsQueryableByRawObjectId` both pin the current behaviour and would be
   deliberately rewritten in the same commit.
2. **Move `ObjectIdBackedIdConverters` into `shared/domain`.** It would pass every rule as
   written (`org.bson` and `org.springframework.data.mongodb.core.convert` are not on the banned
   list). It is also dishonest: the class exists to describe a storage encoding, and putting it in
   the domain to satisfy a rule is the kind of move the fitness functions exist to prevent.
3. **Exempt annotation members from the rule.** Weakest option — the dependency is real, and the
   next thing that reaches infrastructure from the domain via an annotation would slip through.

My recommendation is **1**, done as its own commit with the two pinned tests rewritten alongside
and the rationale in the message. If time is short, **2** is defensible only if the class is
renamed to say what it is and the reasoning is written down; do not take **3**.

## Decisions taken that the next session must not undo

1. **`platform/security` holds the crypto and JWT adapters**, not `identity/infrastructure`.
   `security_types_stop_at_the_api_layer` bans `org.springframework.security..` from every layer
   of every context, and giving BCrypt an exception was rejected in favour of the
   `PasswordHasher` port — a rule with one exception has two eventually. Do not "simplify" by
   injecting `BCryptPasswordEncoder` into identity.
2. **`contexts_do_not_depend_on_the_platform` has exactly one seam:** an `..api..` class may name
   `platform.security` and nothing else of the platform's, pinned by the companion rule
   `api_reaches_only_the_platforms_security_package`.
3. **Published events carry shared-kernel types only**, plus enums from the publisher's own
   domain. `UserRegistered` carries `EmailAddress`/`PersonName`/`Instant`; `ReviewApproved` and
   `ReviewRejected` carry `ReviewId`/`ProductId`/`Rating`/`ReviewStatus`.
4. **The verification mail is `AFTER_COMMIT`**, the opposite of `CatalogStockListener`'s
   `BEFORE_COMMIT`, and `VerificationMailListener`'s javadoc argues why: a mail failure must leave
   a real account, not roll one back. The rating rollup is `BEFORE_COMMIT` for the mirror-image
   reason, argued in `CatalogRatingListener`.
5. **Cross-context reads go through a published port, never an aggregate.**
   `identity.domain.UserDirectory` (display name + favourite ids),
   `ordering.domain.ReviewableOrders` + `ReviewEligibility`, and S11's
   `catalog.domain.ProductSnapshotPort`. No context holds another's aggregate.
6. **The wishlist is split by responsibility, not by accident.** Mutations are
   `identity.api.WishlistController`; `GET /api/wishlist` is
   `catalog.api.WishlistProductsController`, because it returns catalog cards. Two controllers on
   one path is deliberate and both javadocs say so.

## The live-data change that must be reported to the user

`migrate.js` step 10 recomputes every product's rating rollup from its `APPROVED` reviews, as the
plan requires — the old rollup was written at submission time while reviews were still `PENDING`,
so `averageRating` counted reviews that were later rejected while the histogram beside it filtered
`status:'APPROVED'`. The two could never agree.

**Measured against the shipped seed dumps: all 51 products disagree with their own reviews.** The
stored rollups are fabricated demo numbers (several store a fractional `totalRating` — 84.6,
202.5 — in a field the application reads as an `int`). The RTX 4090 stores 30 ratings averaging
4.9 and has 2 approved reviews averaging 4.5. After the recompute the demo storefront's review
counts drop by roughly an order of magnitude.

That is the correct outcome — the product page already contradicted itself, showing "4.9 (30
reviews)" beside a histogram totalling 2 — but it is visible, so it belongs in the outcome section
and in what is said to the user. All 91 seed reviews are `APPROVED` with ratings 2–5, so the
clamp in step 9 is a no-op on the dumps and exists for live databases.

## Remaining plan of work

1. Resolve the `@ValueConverter` finding above. Get to green.
2. `GlobalExceptionHandler`: add the `AccessDeniedDomainException` → 403 handler, and a test
   alongside the existing ones in `GlobalExceptionHandlerTest`.
3. IDOR (#1) and guest-cart binding (#4), with the storefront companion change for the latter.
   Assert the 403 through the real HTTP stack the way `AdminOrderStatusEndpointTest` asserts its
   409, not through a service call.
4. `@PreAuthorize` on the remaining admin controllers.
5. Run `migrate.js` against a real `mongo:6.0` loaded from the actual dumps: confirm step 9 is a
   no-op, step 10 touches 51 products, a second run is a no-op, then re-export
   `gearly.products.json` and re-import clean. This is the evidence the outcome section needs.
6. ArchUnit: tighten onto `reviews..` and `identity..`, and **falsify every new or changed rule**
   by planting a deliberate violation and confirming it fails — S11 found that one of its two new
   rules was inert this way, so this step is not ceremony.
7. Tick every S12 box in `DDD_REFACTORING_PLAN.md` and write the "S12 outcome — shipped" section
   in the house style: what landed, verification actually performed (not asserted), deviations
   from the plan and why, deliberate behaviour changes each with its test edited in the same
   commit, frontend consequences, and what is carried into S13.
8. Delete this file.

## Follow-ups found but deliberately not fixed (log them in the outcome section)

- **A deactivated account keeps its token.** `UserProfileService.deactivate` and
  `AdminUserService.deactivateUser` set `INACTIVE`, and `AuthService.login` refuses an inactive
  account — but `JwtAuthenticationFilter` does not, so an already-issued JWT stays usable for its
  full seven days. Adjacent to the sprint's security items but not one of the four the plan lists;
  a one-line check in the filter would close it.
- **`ProductReviewsDTO.sortBy` is passed to `Sort.by` unvalidated**, so a client picks the sort
  field. Pre-existing, harmless today (an unknown field simply sorts by nothing in Mongo).
- **`UserResponseDTO.favorites` is now `[]` where it used to be `null`** for an account with no
  favourites — the aggregate returns an empty list rather than null. Strictly safer for the
  storefront, which maps over it, but it is a wire change and should be named as one.
