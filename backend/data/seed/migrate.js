// In-place schema migration for the Bookify -> Gearly rename (Sprint 5).
//
// Renames the `books` collection -> `products` and the `bookId` reference key
// -> `productId` everywhere it appears (top-level in reviews/blogPosts, nested
// in carts.items[]). Idempotent: safe to run more than once.
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
  const stale = db.orders.getIndexes().find(i => i.name === "idx_items_bookdId");
  if (stale) {
    db.orders.dropIndex("idx_items_bookdId");
    print("orders: dropped stale index idx_items_bookdId");
  }

  print("Migration complete.");
})();
