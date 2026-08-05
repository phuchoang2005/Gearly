package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.DomainConflictException;

/**
 * An order was asked to move to a status it cannot reach from where it is.
 *
 * <p>Thrown by {@link OrderStatus#assertCanTransitionTo}, and therefore by every write path
 * that changes a status, since they all go through {@code Order.transitionTo}. Maps to
 * <b>409 Conflict</b> via {@link DomainConflictException} — the request was valid, the order
 * is simply not in a state that permits it.
 *
 * <p>The seven {@code /api/admin/orders/{id}/set-*} endpoints are the one place this is
 * caught rather than propagated: they have always answered {@code 200 false} for a refused
 * transition and the admin frontend reads that boolean, so the application service turns it
 * back into {@code false} there. Every other path — {@code PATCH}, {@code PUT}, the customer
 * cancel — lets it through to the 409.
 */
public class IllegalOrderTransitionException extends DomainConflictException {

    private final transient OrderStatus from;
    private final transient OrderStatus to;

    public IllegalOrderTransitionException(OrderStatus from, OrderStatus to) {
        super("An order cannot go from " + from + " to " + to);
        this.from = from;
        this.to = to;
    }

    public OrderStatus getFrom() {
        return from;
    }

    public OrderStatus getTo() {
        return to;
    }
}
