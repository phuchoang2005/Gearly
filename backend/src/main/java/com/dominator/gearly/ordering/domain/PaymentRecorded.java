package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.OrderId;

import java.time.Instant;

/**
 * A transaction was appended to an order's payment: the gateway settling, an administrator
 * marking a delivery paid, a refund opening or closing.
 *
 * <p>Every movement of money against an order raises one, because they all go through
 * {@code Order.recordPayment}. That single funnel is what makes an audit or reconciliation
 * listener possible later without having to find every place a transaction might be appended
 * — which, before S10, was four different services.
 */
public record PaymentRecorded(OrderId orderId,
                              TransactionStatus status,
                              Money amount,
                              Instant occurredOn) implements DomainEvent {
}
