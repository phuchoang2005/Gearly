package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.ReviewEligibility;
import com.dominator.gearly.ordering.domain.ReviewableOrders;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

/**
 * Answers {@link ReviewableOrders} out of the order aggregate. The mirror of
 * {@code CatalogSnapshotService}: a thin application-layer adapter for a port another context
 * consumes.
 */
@Service
@RequiredArgsConstructor
public class ReviewableOrdersService implements ReviewableOrders {

    /**
     * The states in which there is something to have an opinion about: the customer has the
     * goods.
     *
     * <p>{@code DELIVERED} and {@code COMPLETED} only. Deliberately not {@code SHIPPED} — the
     * parcel is in transit and the customer has not seen it — and emphatically not
     * {@code CANCELLED}, which is the case the S8 suite pinned as reviewable and which this
     * exists to refuse. {@code PENDING_REFUND} and {@code REFUNDED} are reachable only from
     * these two, so a customer who reviews and then returns keeps their review; that is
     * deliberate, and it is also what the {@code isReviewed} flag already implied.
     */
    private static final Set<OrderStatus> REVIEWABLE =
            EnumSet.of(OrderStatus.DELIVERED, OrderStatus.COMPLETED);

    private final OrderRepository orders;

    @Override
    public ReviewEligibility eligibilityOf(OrderId orderId, UserId buyer) {
        return orders.findById(orderId)
                .map(order -> eligibilityOf(order, buyer))
                .orElse(ReviewEligibility.NO_SUCH_ORDER);
    }

    @Override
    public void markReviewed(OrderId orderId) {
        orders.findById(orderId).ifPresent(order -> {
            order.markReviewed();
            orders.save(order);
        });
    }

    /**
     * Ownership is checked first, and that ordering is load-bearing: telling a stranger that an
     * order has already been reviewed, or that it has not been delivered, tells them the order
     * exists and what state it is in. {@code NOT_THE_BUYERS} is all they learn.
     */
    private ReviewEligibility eligibilityOf(Order order, UserId buyer) {
        if (!order.isOwnedBy(buyer)) {
            return ReviewEligibility.NOT_THE_BUYERS;
        }
        if (order.isReviewed()) {
            return ReviewEligibility.ALREADY_REVIEWED;
        }
        if (!REVIEWABLE.contains(order.getOrderStatus())) {
            return ReviewEligibility.NOT_YET_DELIVERED;
        }
        return ReviewEligibility.ELIGIBLE;
    }
}
