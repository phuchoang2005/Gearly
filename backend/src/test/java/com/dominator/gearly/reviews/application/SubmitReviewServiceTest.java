package com.dominator.gearly.reviews.application;

import com.dominator.gearly.reviews.domain.RatingOutOfRangeException;
import com.dominator.gearly.reviews.domain.ReviewSubjectNotFoundException;
import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.catalog.domain.ProductSnapshotPort;


import com.dominator.gearly.ordering.domain.ReviewEligibility;
import com.dominator.gearly.ordering.domain.ReviewableOrders;
import com.dominator.gearly.reviews.domain.OrderNotReviewableException;
import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewNotYoursException;
import com.dominator.gearly.reviews.domain.ReviewRepository;
import com.dominator.gearly.reviews.domain.ReviewStatus;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.UserId;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Submitting a review.
 *
 * <p>The successor of the S8 characterization suite's {@code createReview} half. Everything it
 * pinned is still pinned; three things it pinned as bugs are now pinned as fixed, each marked
 * below with the note the S8 suite carried.
 *
 * <p>The rollup assertions are gone from here on purpose, not lost: submitting no longer
 * touches a product at all, and what happens when one is <em>approved</em> is
 * {@code ReviewLifecycleTest}'s and {@code CatalogRatingListenerTest}'s.
 */
@ExtendWith(MockitoExtension.class)
class SubmitReviewServiceTest {

    @Mock private ReviewRepository reviews;
    @Mock private ReviewableOrders orders;
    @Mock private ProductSnapshotPort catalog;

    private SubmitReviewService service;

    private static final String USER_ID = new ObjectId().toHexString();
    private static final String ORDER_ID = new ObjectId().toHexString();
    private static final String PRODUCT_ID = new ObjectId().toHexString();

    @BeforeEach
    void setUp() {
        service = new SubmitReviewService(reviews, orders, catalog);
    }

    private UserId caller() {
        return UserId.of(USER_ID);
    }

    private SubmitReviewsCommand command(int rating) {
        return new SubmitReviewsCommand(ORDER_ID,
                List.of(new SubmitReviewsCommand.Line(PRODUCT_ID, "Great", "Works well", rating)));
    }

    private void orderIs(ReviewEligibility eligibility) {
        when(orders.eligibilityOf(OrderId.of(ORDER_ID), caller())).thenReturn(eligibility);
    }

    private void productExists() {
        lenient().when(catalog.snapshotsOf(List.of(ProductId.of(PRODUCT_ID))))
                .thenReturn(List.of(new CatalogSnapshot(ProductId.of(PRODUCT_ID), "Product",
                        "Maker", Money.of(10.0), null, ProductCondition.NEW, Quantity.of(5))));
    }

    @SuppressWarnings("unchecked")
    private List<Review> captureSaved() {
        ArgumentCaptor<List<Review>> captor = ArgumentCaptor.forClass(List.class);
        verify(reviews).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("the review carries the product, order and author ids as typed ids and starts PENDING")
    void reviewIsBuiltFromTheRequest() {
        orderIs(ReviewEligibility.ELIGIBLE);
        productExists();

        service.submit(caller(), command(5));

        assertThat(captureSaved()).singleElement().satisfies(review -> {
            assertThat(review.getProductId()).isEqualTo(ProductId.of(PRODUCT_ID));
            assertThat(review.getOrderId()).isEqualTo(OrderId.of(ORDER_ID));
            assertThat(review.getUserId()).isEqualTo(UserId.of(USER_ID));
            assertThat(review.getRating()).isEqualTo(Rating.of(5));
            assertThat(review.getSubject()).isEqualTo("Great");
            assertThat(review.getComment()).isEqualTo("Works well");
            assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);
        });
    }

    @Test
    @DisplayName("the order is flagged as reviewed")
    void orderIsFlaggedReviewed() {
        orderIs(ReviewEligibility.ELIGIBLE);
        productExists();

        service.submit(caller(), command(5));

        verify(orders).markReviewed(OrderId.of(ORDER_ID));
    }

    /**
     * <b>FIXED — was a KNOWN BUG.</b> The S8 suite pinned the rollup happening at creation while
     * the review was still {@code PENDING}, which is what made {@code averageRating} and the
     * {@code status:'APPROVED'} histogram structurally unable to agree. Submitting reaches no
     * product now; the catalog reacts to {@code ReviewApproved}.
     */
    @Test
    @DisplayName("FIXED (was a KNOWN INCONSISTENCY): submitting does not move the product's rating")
    void submittingDoesNotTouchTheCatalogsRollup() {
        orderIs(ReviewEligibility.ELIGIBLE);
        productExists();

        service.submit(caller(), command(5));

        // The catalog is consulted only to confirm the product exists — never to change it.
        verify(catalog).snapshotsOf(anyList());
        assertThat(captureSaved()).singleElement()
                .extracting(Review::getStatus).isEqualTo(ReviewStatus.PENDING);
    }

    /**
     * <b>FIXED — was a KNOWN BUG.</b> {@code createReview} wrote {@code order.markReviewed()}
     * and never read the flag back, so a repeated call folded the same stars in again.
     */
    @Test
    @DisplayName("FIXED (was a KNOWN BUG): reviewing the same order twice is refused")
    void doubleReviewIsRefused() {
        orderIs(ReviewEligibility.ALREADY_REVIEWED);

        assertThatThrownBy(() -> service.submit(caller(), command(5)))
                .isInstanceOf(OrderNotReviewableException.class)
                .hasMessageContaining("already reviewed");

        verify(reviews, never()).saveAll(anyList());
    }

    /**
     * <b>FIXED — was a KNOWN BUG.</b> No order status was required, so an order whose goods
     * were never sent could be reviewed.
     */
    @Test
    @DisplayName("FIXED (was a KNOWN BUG): a CANCELLED order is not reviewable")
    void undeliveredOrderIsRefused() {
        orderIs(ReviewEligibility.NOT_YET_DELIVERED);

        assertThatThrownBy(() -> service.submit(caller(), command(5)))
                .isInstanceOf(OrderNotReviewableException.class)
                .hasMessageContaining("delivered");

        verify(reviews, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("an unknown order is a 404")
    void unknownOrderThrows() {
        orderIs(ReviewEligibility.NO_SUCH_ORDER);

        assertThatThrownBy(() -> service.submit(caller(), command(5)))
                .isInstanceOf(ReviewSubjectNotFoundException.class)
                .hasMessage("Order not found, you cannot create review on this.");
    }

    /**
     * Still a 403 with the same message — but raised as {@code ReviewNotYoursException}, a
     * shared-kernel {@code AccessDeniedDomainException}, so the rule is stated without the
     * application layer naming {@code org.springframework.http}.
     */
    @Test
    @DisplayName("reviewing someone else's order is forbidden")
    void otherUsersOrderIsForbidden() {
        orderIs(ReviewEligibility.NOT_THE_BUYERS);

        assertThatThrownBy(() -> service.submit(caller(), command(5)))
                .isInstanceOf(ReviewNotYoursException.class);

        verify(reviews, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("the ownership check happens before any product is looked up")
    void ownershipIsCheckedFirst() {
        orderIs(ReviewEligibility.NOT_THE_BUYERS);

        assertThatThrownBy(() -> service.submit(caller(), command(5)))
                .isInstanceOf(ReviewNotYoursException.class);

        verify(catalog, never()).snapshotsOf(anyList());
    }

    @Test
    @DisplayName("a review for a product that no longer exists is a 404, and nothing is written")
    void unknownProductThrowsAndWritesNothing() {
        orderIs(ReviewEligibility.ELIGIBLE);
        when(catalog.snapshotsOf(List.of(ProductId.of(PRODUCT_ID)))).thenReturn(List.of());

        assertThatThrownBy(() -> service.submit(caller(), command(5)))
                .isInstanceOf(ReviewSubjectNotFoundException.class)
                .hasMessage("Product not found, you cannot create review for this product.");

        verify(reviews, never()).saveAll(anyList());
        verify(orders, never()).markReviewed(any());
    }

    /**
     * The DTO carries {@code @Min}/{@code @Max} now, so a real request is refused at the edge
     * with a field-level message. This is the backstop for a call that reaches the use case
     * some other way: a 400, not the 500 an unmapped {@code IllegalArgumentException} would be.
     */
    @Test
    @DisplayName("FIXED (was a KNOWN BUG): 900 stars is a 400, not a wrecked average")
    void outOfRangeRatingIsRefused() {
        orderIs(ReviewEligibility.ELIGIBLE);
        productExists();

        assertThatThrownBy(() -> service.submit(caller(), command(900)))
                .isInstanceOf(RatingOutOfRangeException.class);

        verify(reviews, never()).saveAll(anyList());
        verify(orders, never()).markReviewed(any());
    }

    @Test
    @DisplayName("FIXED (was a KNOWN BUG): a rating of 0 is a 400")
    void zeroRatingIsRefused() {
        orderIs(ReviewEligibility.ELIGIBLE);
        productExists();

        assertThatThrownBy(() -> service.submit(caller(), command(0)))
                .isInstanceOf(RatingOutOfRangeException.class);

        verify(reviews, never()).saveAll(anyList());
    }
}
