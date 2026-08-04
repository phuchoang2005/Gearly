/**
 * <b>Ordering — core domain.</b> Owns the order lifecycle: placing an order, its status
 * transitions, cancellation and refund initiation, and the payment/transaction record
 * that accumulates against it. The {@code Order} aggregate root is the single place a
 * status may change, so an order cannot be put into an illegal state from any write path.
 *
 * <p><b>Aggregate:</b> {@code Order}, with {@code OrderLine}, {@code ShippingInformation},
 * {@code Payment} and {@code PaymentTransaction} inside its boundary. {@code OrderStatus}
 * owns the transition table. {@code PricingPolicy} is a domain service holding the tax
 * rate and the free-shipping threshold.
 *
 * <p><b>Relationships:</b>
 * <ul>
 *   <li><b>Catalog → Ordering</b> — customer/supplier through the {@code CatalogSnapshot}
 *       anti-corruption layer. An order line copies title, price and image at capture
 *       time and never holds a {@code Product}. Catalog price edits do not rewrite
 *       history.</li>
 *   <li><b>Cart → Ordering</b> — a placed order clears the cart through the
 *       {@code OrderPlaced} event, not a direct call.</li>
 *   <li><b>Ordering → Payments</b> — through the {@code PaymentGateway} port; the MoMo
 *       adapter is an implementation detail this context never names.</li>
 *   <li><b>Ordering → Notification</b> — {@code OrderPlaced} and
 *       {@code OrderStatusChanged} are consumed by a notification listener
 *       {@code AFTER_COMMIT}.</li>
 *   <li><b>Identity → Ordering</b> — orders reference a buyer by {@code UserId} only.</li>
 *   <li><b>Ordering → Analytics</b> — one-way, read-model only.</li>
 * </ul>
 *
 * <p><b>Published events:</b> {@code OrderPlaced}, {@code OrderCancelled},
 * {@code OrderStatusChanged}, {@code PaymentRecorded}.
 *
 * <p>Filled in by <b>Sprint 10</b>.
 */
package com.dominator.gearly.ordering;
