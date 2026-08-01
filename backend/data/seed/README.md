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
