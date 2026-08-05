package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A customer placed an order.
 *
 * <p>Two things have to happen elsewhere when this occurs, and both cross an aggregate
 * boundary: the catalog's stock comes down, and the buyer's cart is emptied of what they just
 * bought. Ordering used to call {@code ProductService} and {@code CartService} directly from
 * inside placement, which meant the ordering context knew the shape of two others.
 *
 * <p>Consumed {@code BEFORE_COMMIT} — see {@code CatalogStockListener} — so the stock write
 * lands inside the same transaction as the order and rolls back with it.
 *
 * <h2>Why it carries a map of ids and quantities rather than the order's lines</h2>
 * S10 put {@code List<OrderLine>} on it, which was fine while the only listener lived in this
 * same context. It stopped being fine the moment the listeners became the contexts that
 * actually react: {@code OrderLine} is ordering's internal type, and a catalog class naming it
 * would be reaching past the published contract into another context's model — exactly what
 * {@code contexts_touch_each_other_only_through_published_types} exists to catch. Every type
 * on this record is a shared-kernel one, so the event is nobody's private vocabulary.
 *
 * <p>Moving the map in here also fixed something. The listener used to build it with
 * {@code Collectors.toMap}, which throws {@code IllegalStateException} on a duplicate key — so
 * an order carrying two lines for the same product, which the admin {@code PUT} path can
 * create, failed placement with an opaque 500. Merging is the obvious right answer and it
 * happens once, here.
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
                          Map<ProductId, Quantity> quantities,
                          Money totalAmount,
                          Instant occurredOn) implements DomainEvent {

    public OrderPlaced {
        quantities = Map.copyOf(quantities);
    }

    public static OrderPlaced from(Order order) {
        return new OrderPlaced(
                order.getUserId(),
                quantitiesOf(order),
                order.getTotalAmount(),
                Instant.now());
    }

    /** Units per product, with repeated lines for one product added together rather than clashing. */
    static Map<ProductId, Quantity> quantitiesOf(Order order) {
        Map<ProductId, Quantity> quantities = new LinkedHashMap<>();
        if (order.getItems() != null) {
            for (OrderLine line : order.getItems()) {
                quantities.merge(line.getProductId(), line.getQuantity(), Quantity::plus);
            }
        }
        return quantities;
    }
}
