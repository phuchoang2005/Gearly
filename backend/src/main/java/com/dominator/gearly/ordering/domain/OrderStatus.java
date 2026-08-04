package com.dominator.gearly.ordering.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Where an order is in its lifecycle, and — the point of this type — <b>which moves between
 * those states are legal</b>.
 *
 * <h2>Why the table lives here</h2>
 * It used to be a {@code private static final Map} on {@code AdminOrderService}, which meant
 * it governed only the one write path that happened to consult it. Three others did not:
 * {@code patchOrder} assigned the status straight from the request body, so
 * {@code PATCH {"orderStatus":"REFUNDED"}} took a {@code PENDING} order to {@code REFUNDED}
 * in one hop; {@code OrderMapper.applyUpsert} did the same for {@code PUT}; and the MoMo
 * callback set {@code PROCESSING}/{@code PENDING} unconditionally. A rule enforced by one of
 * four callers is not a rule.
 *
 * <p>On the enum it is reachable from every caller and impossible to route around: the only
 * way to move an order is {@code Order.transitionTo}, and that asks this table first.
 *
 * <h2>The PENDING_REFUND reconciliation</h2>
 * The old table allowed {@code PENDING_REFUND} only from {@code DELIVERED}, while
 * {@code CustomerOrderService.cancelOrder} drove a paid {@code PENDING} or
 * {@code PROCESSING} order straight there. The two contradicted each other, and the cancel
 * path is the one that describes what actually happens: a customer who has paid and then
 * cancels is owed a refund whether or not the parcel has arrived. The table is widened to
 * match rather than the cancel path narrowed — narrowing it would strand paid cancellations
 * with no way to record the refund. Deliberate behavior change; the S8 characterization
 * test's note is updated in the same commit.
 */
public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    PENDING_REFUND,
    REFUNDED,
    COMPLETED;

    /** Source statuses from which each target may be reached. */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_SOURCES;

    static {
        ALLOWED_SOURCES = new EnumMap<>(OrderStatus.class);
        // The one backwards edge, and it earns its place: a payment gateway reporting a
        // failed checkout returns the order to awaiting payment. The MoMo callback did
        // exactly this by assignment, and the S8 suite pins it. Modelling it as an edge is
        // what lets that path go through the same guard as every other, instead of being a
        // special case the aggregate has to carve out.
        ALLOWED_SOURCES.put(PENDING, EnumSet.of(PROCESSING));
        ALLOWED_SOURCES.put(PROCESSING, EnumSet.of(PENDING));
        ALLOWED_SOURCES.put(SHIPPED, EnumSet.of(PROCESSING));
        ALLOWED_SOURCES.put(DELIVERED, EnumSet.of(SHIPPED));
        ALLOWED_SOURCES.put(COMPLETED, EnumSet.of(DELIVERED));
        ALLOWED_SOURCES.put(CANCELLED, EnumSet.of(PENDING, PROCESSING));
        // widened from EnumSet.of(DELIVERED) — see the class note on the reconciliation
        ALLOWED_SOURCES.put(PENDING_REFUND, EnumSet.of(PENDING, PROCESSING, DELIVERED));
        ALLOWED_SOURCES.put(REFUNDED, EnumSet.of(PENDING_REFUND));
    }

    /**
     * Whether an order in this status may move to {@code target}.
     *
     * <p>A status is never a legal source for itself: re-issuing {@code set-process} on an
     * order that is already {@code PROCESSING} is rejected, exactly as it was before the
     * table moved. {@code Order} is where "the payload merely repeats the current status, so
     * there is nothing to do" is handled — that is an amendment concern, not a transition one.
     */
    public boolean canTransitionTo(OrderStatus target) {
        Set<OrderStatus> allowedSources = ALLOWED_SOURCES.get(target);
        return allowedSources != null && allowedSources.contains(this);
    }

    /** @throws IllegalOrderTransitionException if {@link #canTransitionTo} says no. */
    public void assertCanTransitionTo(OrderStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalOrderTransitionException(this, target);
        }
    }

    /** The statuses an order in this status may move to. Empty once it is final. */
    public Set<OrderStatus> allowedTargets() {
        EnumSet<OrderStatus> targets = EnumSet.noneOf(OrderStatus.class);
        for (OrderStatus target : values()) {
            if (canTransitionTo(target)) {
                targets.add(target);
            }
        }
        return targets;
    }
}
