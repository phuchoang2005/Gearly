// In-place schema migration for the Bookify -> Gearly rename (Sprint 5), the
// Sprint 7 product-timestamp type change, the Sprint 8 optimistic-locking field,
// and the Sprint 9 category/review timestamp normalization.
//
// Renames the `books` collection -> `products` and the `bookId` reference key
// -> `productId` everywhere it appears (top-level in reviews/blogPosts, nested
// in carts.items[]), converts products.addedAt/modifiedAt from ISO-8601
// strings to real BSON Dates (matching Product.addedAt/modifiedAt : Instant, so
// the "newest" sort orders chronologically), backfills the `version` field that
// Product/Order/Cart gained with @Version, and does the same String -> Date
// conversion for categories and reviews. Idempotent: safe to run more than once.
//
// Sprint 9 note: the value objects (Money, typed ids, the enums) deliberately need
// NO migration — their converters write back the same BSON types the documents
// already hold. Step 7 is the one part of S9 that touches stored data.
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

  // 7. categories/reviews addedAt+modifiedAt: String -> BSON Date (Sprint 9).
  //    The same normalization step 5 applied to products in S7, for the two collections
  //    left behind. Category and Review carry Instant fields as of S9, and a String in one
  //    of them fails to map on read.
  //
  //    THREE stored shapes turned up when this was run against the real dumps, not the two
  //    the sprint plan assumed:
  //
  //      A  "2025-12-24T00:00:00.000Z"    zone-qualified ISO      (categories, 10 docs)
  //      B  "2025-05-23T01:57:38.580238"  ISO, no zone, 6 frac    (reviews,    51 docs)
  //      C  "6/9/25, 3:42 AM"             en-US toLocaleString    (reviews,    40 docs)
  //
  //    Shape C is why this step is a client-side loop rather than an aggregation pipeline:
  //    $dateFromString has no format specifier for a 12-hour clock or an AM/PM marker
  //    (no %I, no %p), so the server simply cannot parse it — the first version of this
  //    step aborted mid-collection on the first such document. Shape B also defeats
  //    $toDate, which rejects more than 3 fractional digits.
  //
  //    Every shape is interpreted as UTC and the components are assembled with Date.UTC,
  //    so the result does not depend on the timezone of whoever runs this script. That
  //    matters most for shape C, which carries no zone at all: parsing it with
  //    `new Date(str)` would silently shift it by the operator's local offset.
  //
  //    Type-guarded on $type: "string" and reported per shape. Anything unrecognized is
  //    listed and left untouched rather than being nulled out or guessed at.
  const ISO_SHAPE = /^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2}):(\d{2})(?:\.(\d+))?Z?$/;
  const US_LOCALE_SHAPE = /^(\d{1,2})\/(\d{1,2})\/(\d{2,4}),\s*(\d{1,2}):(\d{2})(?::(\d{2}))?\s*(AM|PM)$/i;

  function parseToUtc(value) {
    let m = ISO_SHAPE.exec(value);
    if (m) {
      // sub-millisecond digits are dropped; a BSON Date has no room for them
      const millis = m[7] ? Number((m[7] + "000").slice(0, 3)) : 0;
      return new Date(Date.UTC(+m[1], +m[2] - 1, +m[3], +m[4], +m[5], +m[6], millis));
    }
    m = US_LOCALE_SHAPE.exec(value);
    if (m) {
      let hour = +m[4] % 12;                              // 12 AM -> 0, 12 PM -> 12
      if (m[7].toUpperCase() === "PM") hour += 12;
      const year = +m[3] < 100 ? 2000 + +m[3] : +m[3];
      return new Date(Date.UTC(year, +m[1] - 1, +m[2], hour, +m[5], +(m[6] || 0)));
    }
    return null;
  }

  for (const c of ["categories", "reviews"]) {
    if (!names.includes(c)) continue;
    for (const field of ["addedAt", "modifiedAt"]) {
      const ops = [];
      const unparsed = [];
      db[c].find({ [field]: { $type: "string" } }, { [field]: 1 }).forEach((doc) => {
        const parsed = parseToUtc(doc[field]);
        if (parsed) {
          ops.push({ updateOne: { filter: { _id: doc._id }, update: { $set: { [field]: parsed } } } });
        } else {
          unparsed.push(doc[field]);
        }
      });
      if (ops.length) db[c].bulkWrite(ops);
      print(`${c}: ${field} string -> Date in ${ops.length} docs`);
      if (unparsed.length) {
        print(`  WARNING: ${unparsed.length} ${c}.${field} values left as strings, e.g. ${unparsed[0]}`);
      }
    }
  }


  print("Migration complete.");
})();
