// In-place schema migration for the Bookify -> Gearly rename (Sprint 5), the
// Sprint 7 product-timestamp type change, and the Sprint 8 optimistic-locking field.
//
// Renames the `books` collection -> `products` and the `bookId` reference key
// -> `productId` everywhere it appears (top-level in reviews/blogPosts, nested
// in carts.items[]), converts products.addedAt/modifiedAt from ISO-8601
// strings to real BSON Dates (matching Product.addedAt/modifiedAt : Instant, so
// the "newest" sort orders chronologically), and backfills the `version` field that
// Product/Order/Cart gained with @Version. Idempotent: safe to run more than once.
//
// Use this when you already have a populated database whose data you want to
// keep (as opposed to re-seeding from the dumps with seed.sh). Run it against
// the database you want to transform:
//
//   mongosh "mongodb://localhost:27017/gearly" migrate.js
//
// To also rename the physical database bookify -> gearly while preserving
// indexes, copy the data first, then run this script against `gearly`:
//
//   mongodump  --uri "mongodb://localhost:27017" --db bookify --archive \
//     | mongorestore --uri "mongodb://localhost:27017" --archive \
//         --nsFrom 'bookify.*' --nsTo 'gearly.*'

(function () {
  // 1. books -> products
  const names = db.getCollectionNames();
  if (names.includes("books")) {
    if (names.includes("products")) db.products.drop();
    db.books.renameCollection("products");
    print("books -> products: renamed");
  } else {
    print("books -> products: skipped (no 'books' collection)");
  }

  // 2. top-level bookId -> productId
  for (const c of ["reviews", "blogPosts"]) {
    const res = db[c].updateMany(
      { bookId: { $exists: true } },
      { $rename: { bookId: "productId" } }
    );
    print(`${c}: bookId -> productId in ${res.modifiedCount} docs`);
  }

  // 3. nested items[].bookId -> items[].productId (carts)
  const res = db.carts.updateMany(
    { "items.bookId": { $exists: true } },
    [{
      $set: {
        items: {
          $map: {
            input: "$items",
            as: "it",
            in: {
              $mergeObjects: [
                { $arrayToObject: {
                    $filter: {
                      input: { $objectToArray: "$$it" },
                      cond: { $ne: ["$$this.k", "bookId"] }
                    }
                } },
                { $cond: [
                    { $ne: [{ $type: "$$it.bookId" }, "missing"] },
                    { productId: "$$it.bookId" },
                    {}
                ] }
              ]
            }
          }
        }
      }
    }]
  );
  print(`carts: items[].bookId -> productId in ${res.modifiedCount} docs`);

  // 4. drop the historical index-name typo if present
  //    (Spring recreates it as idx_items_productId on boot)
  if (names.includes("orders")) {
    const stale = db.orders.getIndexes().find(i => i.name === "idx_items_bookdId");
    if (stale) {
      db.orders.dropIndex("idx_items_bookdId");
      print("orders: dropped stale index idx_items_bookdId");
    }
  }

  // 5. products.addedAt/modifiedAt: ISO-8601 String -> BSON Date (Sprint 7).
  //    Type-guarded so re-runs skip the already-converted Date values.
  for (const field of ["addedAt", "modifiedAt"]) {
    const res = db.products.updateMany(
      { [field]: { $type: "string" } },
      [{ $set: { [field]: { $toDate: "$" + field } } }]
    );
    print(`products: ${field} string -> Date in ${res.modifiedCount} docs`);
  }

  // 6. products/orders/carts: backfill the optimistic-locking version (Sprint 8).
  //    Product, Order and Cart gained @Version. Spring Data reads a MISSING version as
  //    null and treats a null version as "not yet persisted", so the first save() of a
  //    pre-S8 document would be issued as an insert and fail on the duplicate _id.
  //    Seeding 0 makes those documents update normally. Guarded on $exists, so re-runs
  //    skip documents that already carry a version and never reset a live counter.
  for (const c of ["products", "orders", "carts"]) {
    if (!names.includes(c)) continue;
    const res = db[c].updateMany(
      { version: { $exists: false } },
      { $set: { version: NumberLong("0") } } // string arg: numeric NumberLong() is deprecated
    );
    print(`${c}: version backfilled in ${res.modifiedCount} docs`);
  }

  print("Migration complete.");
})();
