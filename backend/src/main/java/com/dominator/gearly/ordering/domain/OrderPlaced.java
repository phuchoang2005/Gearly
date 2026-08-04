package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.UserId;

import java.time.Instant;
import java.util.List;

/**
 * A customer placed an order.
 *
 * <p>Two things have to happen elsewhere when this occurs, and both cross an aggregate
 * boundary: the catalog's stock comes down, and the buyer's cart is emptied of what they just
 * bought. Ordering used to call {@code ProductService} and {@code CartService} directly from
 * inside placement, which meant the ordering context knew the shape of two others.
 *
 * <p>Consumed {@code BEFORE_COMMIT} — see {@code OrderPlacedListener} — so the stock write
 * lands inside the same transaction as the order and rolls back with it.
 *
 * <h2>Why there is no order id on it</h2>
 * Because there is not one to put there yet. Identity is assigned by MongoDB on insert, and
 * this event is raised by the application service the moment that insert returns, from the
 * saved aggregate — so a listener that needed an id could have one. Nothing that reacts to
 * placement does: the stock decrement and the cart clear both key off the lines and the buyer.
 * Adding it means deciding to assign identity in the domain instead of in the database, which
 * is a change worth making on its own and not as a side effect of introducing events.
 */
public record OrderPlaced(UserId userId,
                          List<OrderLine> lines,
                          Money totalAmount,
                          Instant occurredOn) implements DomainEvent {

    public OrderPlaced {
        lines = List.copyOf(lines);
    }

    public static OrderPlaced from(Order order) {
        return new OrderPlaced(order.getUserId(), order.getItems(), order.getTotalAmount(), Instant.now());
    }
}
