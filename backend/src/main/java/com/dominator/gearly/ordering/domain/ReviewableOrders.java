package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;

/**
 * <b>Ordering's published interface to the reviews context.</b> Two questions and nothing else:
 * may this person review this order, and please record that they have.
 *
 * <p>The counterpart of {@code catalog.domain.ProductSnapshotPort} and
 * {@code identity.domain.UserDirectory}, and there for the same reason. {@code ReviewService}
 * held an {@code OrderRepository} and worked directly on the {@code Order} aggregate — asking
 * it who owned it, flipping its {@code isReviewed} flag, and saving it. That is one context
 * writing to another's aggregate, which is the coupling
 * {@code contexts_touch_each_other_only_through_published_types} exists to refuse, and it was
 * only invisible because {@code ReviewService} was not in a context yet.
 *
 * <p>Ordering owns the answer anyway. Whether a purchase can be reviewed depends on what state
 * the order reached and whether it has been reviewed before — both facts about an order, and
 * neither one the reviews context could evaluate without holding the aggregate.
 */
public interface ReviewableOrders {

    /** Whether {@code buyer} may review {@code orderId}, and if not, why not. */
    ReviewEligibility eligibilityOf(OrderId orderId, UserId buyer);

    /**
     * Record that the order has been reviewed, so a second attempt is refused.
     *
     * <p>Idempotent and silent on a missing order: the caller has already been told the order
     * exists by {@link #eligibilityOf}, and there is nothing useful to do about it having been
     * deleted in between.
     */
    void markReviewed(OrderId orderId);
}
