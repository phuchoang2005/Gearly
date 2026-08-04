/**
 * <b>Catalog — core domain.</b> Owns what is for sale and how much of it there is:
 * product identity, description, pricing, condition, categorization, imagery, the stock
 * level, and the rating roll-up shown on a product page.
 *
 * <p><b>Aggregates:</b> {@code Product} (root — {@code reserve}, {@code restock},
 * {@code changePrice}, {@code addRating}, with stock and rating as invariants of the
 * aggregate rather than rules scattered across callers) and {@code Category}.
 *
 * <p><b>Relationships:</b>
 * <ul>
 *   <li><b>Catalog → Ordering, Catalog → Cart</b> — upstream in a customer/supplier
 *       relationship, mediated by the {@code CatalogSnapshot} ACL and the
 *       {@code ProductSnapshotPort}. Downstream contexts get a snapshot, never a
 *       {@code Product}.</li>
 *   <li><b>Reviews → Catalog</b> — the rating roll-up is applied by a
 *       {@code ReviewApproved} event handler, so only moderated reviews count.</li>
 *   <li><b>Catalog → Analytics</b> — one-way, read-model only.</li>
 * </ul>
 *
 * <p><b>Invariant this context exists to protect:</b> stock is decremented in exactly one
 * place. Today the same check is written five times across three services; those collapse
 * onto {@code Product.reserve(Quantity)}.
 *
 * <p>Filled in by <b>Sprint 11</b>.
 */
package com.dominator.gearly.catalog;
