package com.dominator.gearly.ordering.domain;

/**
 * The state of one movement of money against an order.
 *
 * <p>Distinct from {@link OrderStatus} even where the names coincide: an order is
 * {@code PENDING_REFUND} while a refund it owes is outstanding, whereas a
 * {@code PENDING_REFUND} <i>transaction</i> is the individual record of that obligation.
 */
public enum TransactionStatus {
    PENDING,
    SUCCESSFUL,
    FAILED,
    PENDING_REFUND,
    REFUNDED
}
