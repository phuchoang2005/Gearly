package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.ReviewId;

import java.time.Instant;

/**
 * A moderator refused a review, or withdrew one already published.
 *
 * <p>Raised on <em>every</em> rejection, not only on the ones that had been counted. The
 * alternative — raising it only from {@code APPROVED}, where there is a rollup to undo — would
 * have made the event a description of the listener's needs rather than of what happened, and
 * the first consumer that cared about rejections for another reason (a moderation audit, a
 * notification to the author) would have found it silently incomplete.
 *
 * <p>{@link #previousStatus} carries the fact that decides it: the catalog subtracts the stars
 * only when the review it is losing was {@code APPROVED}. A {@code PENDING → REJECTED} review
 * never counted, so there is nothing to take away, and a listener that subtracted anyway would
 * drag the product's average down by a review nobody ever saw.
 */
public record ReviewRejected(ReviewId reviewId,
                             ProductId productId,
                             Rating rating,
                             ReviewStatus previousStatus,
                             Instant occurredOn) implements DomainEvent {

    /** Whether this rejection takes a rating away from the product, or merely fails to add one. */
    public boolean withdrawsACountedRating() {
        return previousStatus != null && previousStatus.countsTowardsRating();
    }
}
