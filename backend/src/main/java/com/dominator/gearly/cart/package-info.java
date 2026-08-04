/**
 * <b>Cart — supporting.</b> Owns the pre-checkout basket for both signed-in users and
 * guests: adding and re-quantifying lines, merging a guest basket into a user basket at
 * login, and reconciling lines against current catalog stock.
 *
 * <p><b>Aggregate:</b> {@code Cart} (root) with {@code CartLine}. Invariants:
 * a cart belongs to a {@code UserId} <em>xor</em> a guest id, never both and never
 * neither; a line's quantity is positive and never exceeds available stock.
 *
 * <p><b>Relationships:</b>
 * <ul>
 *   <li><b>Catalog → Cart</b> — customer/supplier through {@code CatalogSnapshot}.
 *       Line price and title are hydrated from the catalog, never from the request body;
 *       this is what closes the current price-tampering hole.</li>
 *   <li><b>Cart → Ordering</b> — the cart is emptied by an {@code OrderPlaced} listener
 *       running {@code BEFORE_COMMIT}, inside the placing transaction.</li>
 * </ul>
 *
 * <p>Filled in by <b>Sprint 11</b>.
 */
package com.dominator.gearly.cart;
