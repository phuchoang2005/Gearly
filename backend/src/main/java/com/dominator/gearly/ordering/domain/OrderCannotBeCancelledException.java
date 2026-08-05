package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.DomainConflictException;

/**
 * A customer asked to cancel an order that has gone too far to be cancelled. Once a parcel
 * has shipped the money question becomes a refund, not a cancellation.
 *
 * <p>Maps to <b>409 Conflict</b>, the status {@code CustomerOrderService} already returned
 * for this case by throwing {@code ConflictException} — same response, but thrown by the
 * aggregate that owns the rule rather than by whichever service happened to check it.
 */
public class OrderCannotBeCancelledException extends DomainConflictException {

    private final transient OrderStatus status;

    public OrderCannotBeCancelledException(OrderStatus status) {
        super("This order already has status that cannot be cancelled: " + status);
        this.status = status;
    }

    public OrderStatus getStatus() {
        return status;
    }
}
