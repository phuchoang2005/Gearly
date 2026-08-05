package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.ReviewId;

import java.time.Instant;

/**
 * A moderator published a review.
 *
 * <p><b>This is where a product's rating rollup comes from now.</b> It used to happen at
 * submission, inside {@code ReviewService.createReview}, while the review was still
 * {@code PENDING} — so {@code averageRating} counted reviews that were later rejected and
 * never published, while the star histogram beside it queried
 * {@code {status: 'APPROVED'}}. The two numbers on the same product page were computed from
 * different sets of reviews and could not agree. The plan calls this out as structurally
 * inconsistent, and it is: no amount of recomputing fixes a rollup fed by the wrong event.
 *
 * <p>Consumed by {@code catalog.application.CatalogRatingListener}, which calls
 * {@code Product.addRating}. Every field is a shared-kernel type, as a published event's must
 * be — {@link Rating} rather than a bare {@code int} is also what makes the value 1–5 by the
 * time the catalog sees it.
 *
 * <h2>Why {@code previousStatus} is on it</h2>
 * So that the consumer can tell a first publication from a reinstatement without holding the
 * review. It is a {@link ReviewStatus} — an enum from the publisher's own domain, which the
 * fitness function permits precisely because it is part of reviews' published language. In
 * practice every arrival at {@code APPROVED} is a first one, because the lifecycle refuses a
 * self-transition; carrying the source status is what lets the listener assert that rather than
 * assume it.
 */
public record ReviewApproved(ReviewId reviewId,
                             ProductId productId,
                             Rating rating,
                             ReviewStatus previousStatus,
                             Instant occurredOn) implements DomainEvent {
}
