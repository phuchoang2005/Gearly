package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.ReviewEligibility;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Ordering's answer to "may this person review this order?".
 *
 * <p>This is where the two S8 {@code KNOWN BUG}s are actually decided — the reviews context only
 * turns the answer into a status code. Both were bugs of omission: {@code createReview} asked
 * who owned the order and nothing else.
 */
@ExtendWith(MockitoExtension.class)
class ReviewableOrdersServiceTest {

    @Mock private OrderRepository orders;

    private ReviewableOrdersService service;

    private static final String ORDER_ID = new ObjectId().toHexString();
    private static final String BUYER = new ObjectId().toHexString();

    @BeforeEach
    void setUp() {
        service = new ReviewableOrdersService(orders);
    }

    private Order order(OrderStatus status, boolean reviewed) {
        OrderFixture.Builder builder = OrderFixture.anOrder()
                .withId(ORDER_ID).ownedBy(BUYER).at(status);
        if (reviewed) {
            builder.reviewed();
        }
        return builder.build();
    }

    private ReviewEligibility eligibilityOf(OrderStatus status, boolean reviewed) {
        when(orders.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order(status, reviewed)));
        return service.eligibilityOf(OrderId.of(ORDER_ID), UserId.of(BUYER));
    }

    @Test
    void aDeliveredOrderTheBuyerHasNotReviewedIsEligible() {
        assertThat(eligibilityOf(OrderStatus.DELIVERED, false)).isEqualTo(ReviewEligibility.ELIGIBLE);
    }

    @Test
    void aCompletedOrderIsEligibleToo() {
        assertThat(eligibilityOf(OrderStatus.COMPLETED, false)).isEqualTo(ReviewEligibility.ELIGIBLE);
    }

    @Test
    @DisplayName("FIXED (was a KNOWN BUG): a CANCELLED order is not reviewable")
    void aCancelledOrderIsNotReviewable() {
        assertThat(eligibilityOf(OrderStatus.CANCELLED, false))
                .isEqualTo(ReviewEligibility.NOT_YET_DELIVERED);
    }

    @Test
    @DisplayName("nor is one that has been paid for but not yet sent")
    void aPendingOrderIsNotReviewable() {
        assertThat(eligibilityOf(OrderStatus.PENDING, false))
                .isEqualTo(ReviewEligibility.NOT_YET_DELIVERED);
    }

    @Test
    @DisplayName("nor is one still in transit — the customer has not seen it")
    void aShippedOrderIsNotReviewable() {
        assertThat(eligibilityOf(OrderStatus.SHIPPED, false))
                .isEqualTo(ReviewEligibility.NOT_YET_DELIVERED);
    }

    @Test
    @DisplayName("FIXED (was a KNOWN BUG): an order already reviewed cannot be reviewed again")
    void anAlreadyReviewedOrderIsRefused() {
        assertThat(eligibilityOf(OrderStatus.DELIVERED, true))
                .isEqualTo(ReviewEligibility.ALREADY_REVIEWED);
    }

    @Test
    void anUnknownOrderSaysSo() {
        when(orders.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

        assertThat(service.eligibilityOf(OrderId.of(ORDER_ID), UserId.of(BUYER)))
                .isEqualTo(ReviewEligibility.NO_SUCH_ORDER);
    }

    /**
     * Ownership is answered before anything else, and that ordering is deliberate: telling a
     * stranger the order has already been reviewed would tell them it exists and what state it
     * is in.
     */
    @Test
    @DisplayName("a stranger learns only that the order is not theirs")
    void aStrangerLearnsNothingElse() {
        when(orders.findById(OrderId.of(ORDER_ID)))
                .thenReturn(Optional.of(order(OrderStatus.DELIVERED, true)));

        assertThat(service.eligibilityOf(OrderId.of(ORDER_ID), UserId.of(new ObjectId().toHexString())))
                .isEqualTo(ReviewEligibility.NOT_THE_BUYERS);
    }

    @Test
    void markReviewedFlagsAndSavesTheOrder() {
        Order order = order(OrderStatus.DELIVERED, false);
        when(orders.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.of(order));

        service.markReviewed(OrderId.of(ORDER_ID));

        assertThat(order.isReviewed()).isTrue();
        verify(orders).save(order);
    }

    @Test
    @DisplayName("marking an order that has vanished is silent, not an error")
    void markReviewedOnAMissingOrderIsSilent() {
        when(orders.findById(OrderId.of(ORDER_ID))).thenReturn(Optional.empty());

        service.markReviewed(OrderId.of(ORDER_ID));

        verify(orders, never()).save(any());
    }
}
