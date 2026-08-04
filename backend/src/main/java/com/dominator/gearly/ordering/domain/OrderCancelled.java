package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;

import java.time.Instant;

/**
 * A customer cancelled an order.
 *
 * <p>{@code refundOwed} is the part a listener actually cares about: a cancellation of a paid
 * order does not end the matter, it opens a refund and leaves the order in
 * {@code PENDING_REFUND} for an administrator to settle. An unpaid one is simply over.
 *
 * <p>The obvious future listener is the one that returns the reserved stock to the catalog.
 * S11 owns that, along with the {@code Product.restock(Quantity)} it needs — today nothing
 * gives the units back, which is a real bug and deliberately not fixed in this sprint, whose
 * job was to make the write paths honest rather than to add behavior.
 */
public record OrderCancelled(OrderId orderId,
                             UserId userId,
                             OrderStatus resultingStatus,
                             boolean refundOwed,
                             String reason,
                             Instant occurredOn) implements DomainEvent {
}
