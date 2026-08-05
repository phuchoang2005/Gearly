package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;

import java.time.Instant;
import java.util.Map;

/**
 * A customer cancelled an order.
 *
 * <p>{@code refundOwed} is the part a payment listener cares about: cancelling a paid order
 * does not end the matter, it opens a refund and leaves the order in {@code PENDING_REFUND}
 * for an administrator to settle. An unpaid one is simply over.
 *
 * <p>{@code quantities} is the part the catalog cares about, and it is why this event now
 * carries one. S10 shipped without it and said so plainly: "nothing gives the units back,
 * which is a real bug and deliberately not fixed in this sprint". {@code CatalogStockListener}
 * fixes it here, by putting exactly these units back on the shelf with
 * {@code Product.restock}. Same shape as {@link OrderPlaced#quantities()}, and shared-kernel
 * types for the same reason — a catalog listener may not name an {@code OrderLine}.
 */
public record OrderCancelled(OrderId orderId,
                             UserId userId,
                             Map<ProductId, Quantity> quantities,
                             OrderStatus resultingStatus,
                             boolean refundOwed,
                             String reason,
                             Instant occurredOn) implements DomainEvent {

    public OrderCancelled {
        quantities = Map.copyOf(quantities);
    }
}
