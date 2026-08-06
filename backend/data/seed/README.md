# Seed data & Bookify → Gearly migration

MongoDB collection dumps (exported as JSON) used to seed a fresh **Gearly**
database, plus the repeatable migration for existing Bookify databases.

Relocated here from `src/main/java/com/dominator/gearly/data/` (Sprint 1) so
they no longer sit inside the Java source tree. Kept in-repo intentionally.

## Files

- `gearly.<collection>.json` — one dump per collection. `gearly.products.json`
  is the former `books` collection; the embedded/top-level product reference key
  is `productId` (renamed from `bookId` in Sprint 5).
- `seed.sh` — import every dump into a fresh database (drops existing collections).
- `migrate.js` — in-place migration for a **populated** Bookify database you want
  to keep: renames `books` → `products` and `bookId` → `productId`. Idempotent.

## Seed a fresh database (recommended)

```bash
# host must have mongoimport (MongoDB Database Tools)
MONGO_URI="mongodb://localhost:27017/gearly" ./seed.sh
```

Or, without any host tooling, via Docker/Colima:

```bash
make -C .. seed            # from backend/, seeds the Dockerized mongo
```

## Migrate an existing Bookify database (keep live data)

```bash
# rename the physical database first (preserves indexes)
mongodump  --uri "mongodb://localhost:27017" --db bookify --archive \
  | mongorestore --uri "mongodb://localhost:27017" --archive \
      --nsFrom 'bookify.*' --nsTo 'gearly.*'

# then apply the schema rename (books->products, bookId->productId)
mongosh "mongodb://localhost:27017/gearly" migrate.js
```

> **Snapshot the database before migrating.** `mongodump` above doubles as the
> snapshot; keep the archive to roll back.

### Two S12 steps you should know about before running it

Both are in `migrate.js` with the full reasoning; this is the short version.

- **Step 10 recomputes every product's rating rollup from its `APPROVED` reviews,** and on the
  shipped dumps that changed all 51 products — the stored numbers were demo values rather than
  the sum of anything. Displayed review counts drop accordingly. The rollup used to be written
  at submission time while reviews were still `PENDING`, so `averageRating` counted reviews a
  moderator later rejected, while the histogram beside it filters `status:'APPROVED'`. The two
  could never agree; now they do.
- **Step 11 converts `reviews.productId`/`orderId`/`userId` from `ObjectId` to `String`.**
  **Run it before starting the application.** Those were the last fields storing a typed id as an
  `ObjectId` while the same type is a plain string everywhere else; the converters that absorbed
  the difference are gone, so an unconverted document no longer maps on read.

The dumps in this directory ship already migrated — re-running `migrate.js` against a database
seeded from them prints zero for every step.
