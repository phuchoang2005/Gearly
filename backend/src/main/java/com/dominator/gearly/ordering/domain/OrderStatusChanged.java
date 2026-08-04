package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.OrderId;

import java.time.Instant;

/**
 * An order moved along its lifecycle. Raised by {@code Order} itself, which is why it can only
 * ever describe a move the transition table permitted.
 *
 * <p>Nothing listens to it yet inside the backend. It is published because it is the event the
 * Notification context consumes in S13 — "your order has shipped" — and because publishing it
 * from the aggregate now means that listener needs no change to the ordering context at all.
 */
public record OrderStatusChanged(OrderId orderId,
                                 OrderStatus from,
                                 OrderStatus to,
                                 Instant occurredOn) implements DomainEvent {
}
