package com.dominator.gearly.reviews.application;

import com.dominator.gearly.catalog.domain.ProductSnapshotPort;
import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.ordering.domain.ReviewEligibility;
import com.dominator.gearly.ordering.domain.ReviewableOrders;
import com.dominator.gearly.reviews.domain.OrderNotReviewableException;
import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewNotYoursException;
import com.dominator.gearly.reviews.domain.ReviewRepository;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A customer reviews what they bought.
 *
 * <h2>What this no longer does</h2>
 * It no longer touches the catalog. {@code ReviewService.createReview} loaded every reviewed
 * product, called {@code addRating} on each, and saved them alongside the reviews — so a
 * product's average moved the moment a review was written, while the review itself sat
 * {@code PENDING} and might never be published. The rollup follows moderation now; see
 * {@code ReviewApproved}.
 *
 * <p>It also no longer holds an {@code OrderRepository}. Whether this purchase can be reviewed
 * is a fact about the order, and ordering answers it through {@link ReviewableOrders} — which
 * is what closes the two S8 {@code KNOWN BUG}s in one place: a {@code CANCELLED} order is no
 * longer reviewable, and reviewing the same order twice is refused instead of folding the same
 * stars in again.
 */
@Service
@RequiredArgsConstructor
public class SubmitReviewService {

    private final ReviewRepository reviews;
    private final ReviewableOrders orders;
    private final ProductSnapshotPort catalog;

    @Transactional
    public void submit(UserId caller, SubmitReviewsCommand command) {
        OrderId orderId = OrderId.of(command.orderId());
        requireReviewable(orderId, caller);
        requireKnownProducts(command);

        List<Review> submitted = new ArrayList<>();
        for (SubmitReviewsCommand.Line line : command.lines()) {
            submitted.add(Review.submit(
                    ProductId.of(line.productId()),
                    orderId,
                    caller,
                    ratingOf(line.rating()),
                    line.subject(),
                    line.comment()));
        }

        reviews.saveAll(submitted);
        orders.markReviewed(orderId);
    }

    /**
     * Each refusal answers with the status that describes it: 404 for an order that is not
     * there, 403 for one that is not the caller's, 400 for one that is theirs but cannot be
     * reviewed yet or has been already.
     */
    private void requireReviewable(OrderId orderId, UserId caller) {
        ReviewEligibility eligibility = orders.eligibilityOf(orderId, caller);
        switch (eligibility) {
            case ELIGIBLE -> { }
            case NO_SUCH_ORDER -> throw new ResourceNotFoundException(
                    "Order not found, you cannot create review on this.");
            case NOT_THE_BUYERS -> throw new ReviewNotYoursException();
            default -> throw new OrderNotReviewableException(eligibility);
        }
    }

    /**
     * Nothing is written if any product has been delisted, which is the behaviour the S8 suite
     * pins. One batched read through the catalog's published port, where the old code loaded
     * the {@code Product} aggregates it was about to mutate.
     */
    private void requireKnownProducts(SubmitReviewsCommand command) {
        List<ProductId> wanted = command.lines().stream()
                .map(SubmitReviewsCommand.Line::productId)
                .map(ProductId::of)
                .toList();

        Set<ProductId> known = catalog.snapshotsOf(wanted).stream()
                .map(snapshot -> snapshot.productId())
                .collect(Collectors.toSet());

        if (!known.containsAll(wanted)) {
            throw new ResourceNotFoundException(
                    "Product not found, you cannot create review for this product.");
        }
    }

    /**
     * The star count, as a value the domain will accept.
     *
     * <p>{@code CreateReviewRequestDTO} carries {@code @Min(1) @Max(5)} now, so a real request
     * is refused at the edge with a field-level message — which is the half of this the plan
     * assigns to S12. This is the other half: a 400 rather than the 500 an unmapped
     * {@code IllegalArgumentException} would be, for any path that reaches the use case
     * without passing bean validation.
     */
    private Rating ratingOf(int value) {
        try {
            return Rating.of(value);
        } catch (IllegalArgumentException outOfRange) {
            throw new BadRequestException(outOfRange.getMessage());
        }
    }
}
