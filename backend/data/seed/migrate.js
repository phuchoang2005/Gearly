// In-place schema migration for the Bookify -> Gearly rename (Sprint 5), the
// Sprint 7 product-timestamp type change, the Sprint 8 optimistic-locking field,
// the Sprint 9 category/review timestamp normalization, the Sprint 11
// product rating-rollup repair, and the Sprint 12 review-rating clamp and
// rating-rollup recompute.
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


  // ---------------------------------------------------------------------------
  // Step 8 (S11) - repair any product rating rollup that cannot be true.
  //
  // Product.addRating goes through the ProductRating value object now, which
  // refuses a rollup where the star total is impossible for the number of
  // ratings (below 1x or above 5x the count), or where a total exists with no
  // ratings behind it. That invariant is what stops the three fields drifting
  // apart again - but it also means a document already holding an impossible
  // rollup would throw the moment somebody reviewed that product.
  //
  // Such documents are reachable: until S11 the rating arrived as an unchecked
  // int, so a review of 900 stars was folded straight into the total. The S8
  // characterization suite pinned exactly that as a KNOWN BUG.
  //
  // The repair clamps the total into the range the count allows and recomputes
  // the average with the same arithmetic the aggregate uses. Idempotent, and a
  // no-op on consistent data - all 51 seed products are already consistent, so
  // this step exists for live databases rather than for the dumps.
  if (names.includes("products")) {
    const ops = [];
    db.products
      .find(
        {},
        { ratingCount: 1, totalRating: 1, averageRating: 1 }
      )
      .forEach((doc) => {
        const count = doc.ratingCount || 0;
        const total = doc.totalRating || 0;

        let fixedCount = count < 0 ? 0 : count;
        let fixedTotal = total < 0 ? 0 : total;

        if (fixedCount === 0) {
          fixedTotal = 0;
        } else {
          if (fixedTotal < fixedCount) fixedTotal = fixedCount;         // below 1 star each
          if (fixedTotal > fixedCount * 5) fixedTotal = fixedCount * 5; // above 5 stars each
        }

        // the same Math.round(avg * 100) / 100 the rollup has always used
        const fixedAverage =
          fixedCount === 0 ? 0 : Math.round((fixedTotal / fixedCount) * 100) / 100;

        if (
          fixedCount !== count ||
          fixedTotal !== total ||
          fixedAverage !== (doc.averageRating || 0)
        ) {
          ops.push({
            updateOne: {
              filter: { _id: doc._id },
              update: {
                $set: {
                  ratingCount: fixedCount,
                  totalRating: fixedTotal,
                  averageRating: fixedAverage,
                },
              },
            },
          });
        }
      });

    if (ops.length) db.products.bulkWrite(ops);
    print(`products: rating rollup repaired in ${ops.length} docs`);
  }

  // ---------------------------------------------------------------------------
  // Step 9 (S12) - clamp any review rating that is not a legal star count.
  //
  // Review.rating is a Rating value object now, and Rating is 1..5 by
  // construction. S9 built the type and deliberately left this field an int,
  // because a document holding an out-of-range value had to stay readable until
  // something owned the reviews context. Something does, so the field changed -
  // and from this point a stored rating outside 1..5 does not deserialize at
  // all. It is not a bad number on a screen; the document becomes unreadable and
  // every query that touches it fails.
  //
  // Such documents are reachable: until S11 the rating arrived as an unchecked
  // int and was folded straight into the product's running total, which is what
  // the S8 characterization suite pinned as a 900-star review. The seed dumps
  // are clean (all 91 reviews are 2..5), so this step exists for live databases.
  //
  // Clamping rather than deleting: the review's text is the customer's and is
  // still worth reading, and a rating outside the scale carries no information
  // about which end of it was meant. Idempotent - a second run finds nothing.
  if (names.includes("reviews")) {
    const ops = [];
    db.reviews
      .find(
        {
          $or: [
            { rating: { $lt: 1 } },
            { rating: { $gt: 5 } },
            { rating: { $not: { $type: "int" } } },
          ],
        },
        { rating: 1 }
      )
      .forEach((doc) => {
        const raw = typeof doc.rating === "number" ? Math.round(doc.rating) : 0;
        const value = raw < 1 ? 1 : raw > 5 ? 5 : raw;
        ops.push({
          updateOne: {
            filter: { _id: doc._id },
            update: { $set: { rating: new NumberInt(value) } },
          },
        });
      });

    if (ops.length) db.reviews.bulkWrite(ops);
    print(`reviews: ratings clamped into 1..5 in ${ops.length} docs`);
  }

  // ---------------------------------------------------------------------------
  // Step 10 (S12) - recompute every product's rating rollup from its APPROVED
  // reviews.
  //
  // WHY THIS IS NOT COSMETIC. The rollup used to be written at submission time:
  // ReviewService.createReview called product.addRating(...) in the same loop
  // that created the reviews, while every one of them was still PENDING. So
  // averageRating counted reviews a moderator later rejected and nobody ever
  // saw - while the star histogram on the same product page is drawn from an
  // aggregation that filters { status: 'APPROVED' }. The two numbers were
  // computed from different sets of reviews by design, so they could never agree.
  //
  // S12 moves the rollup onto a ReviewApproved domain event, which fixes it
  // going forward. This step fixes what is already stored, and it must run once
  // for the two numbers to start out consistent.
  //
  // WHAT IT WILL DO TO THE DEMO DATA, stated plainly because it is visible:
  // in the shipped seed dumps every one of the 51 products disagrees with its
  // own reviews, because the stored rollups are fabricated marketing numbers
  // rather than the sum of anything (several store a fractional totalRating -
  // 84.6, 202.5 - in a field the application reads as an int). The RTX 4090, for
  // instance, stores 30 ratings averaging 4.9 and has 2 approved reviews
  // averaging 4.5. After this step the displayed review counts drop to what the
  // reviews collection actually contains. That is the point: the product page
  // already contradicted itself, showing "4.9 (30 reviews)" beside a histogram
  // totalling 2.
  //
  // Idempotent by construction - a recompute of a correct rollup is a no-op.
  // Run step 9 first: this one counts only ratings within 1..5.
  if (names.includes("products") && names.includes("reviews")) {
    const tally = {};
    db.reviews
      .find({ status: "APPROVED" }, { productId: 1, rating: 1 })
      .forEach((review) => {
        const stars = review.rating;
        if (typeof stars !== "number" || stars < 1 || stars > 5) return;
        const key = String(review.productId);
        if (!tally[key]) tally[key] = { count: 0, total: 0 };
        tally[key].count += 1;
        tally[key].total += stars;
      });

    const ops = [];
    db.products
      .find({}, { ratingCount: 1, totalRating: 1, averageRating: 1 })
      .forEach((product) => {
        const rollup = tally[String(product._id)] || { count: 0, total: 0 };
        // the same Math.round(avg * 100) / 100 ProductRating.average() uses
        const average =
          rollup.count === 0
            ? 0
            : Math.round((rollup.total / rollup.count) * 100) / 100;

        if (
          rollup.count !== (product.ratingCount || 0) ||
          rollup.total !== (product.totalRating || 0) ||
          average !== (product.averageRating || 0)
        ) {
          ops.push({
            updateOne: {
              filter: { _id: product._id },
              update: {
                $set: {
                  ratingCount: rollup.count,
                  totalRating: rollup.total,
                  averageRating: average,
                },
              },
            },
          });
        }
      });

    if (ops.length) db.products.bulkWrite(ops);
    print(
      `products: rating rollup recomputed from APPROVED reviews in ${ops.length} docs`
    );
  }

  print("Migration complete.");
})();
