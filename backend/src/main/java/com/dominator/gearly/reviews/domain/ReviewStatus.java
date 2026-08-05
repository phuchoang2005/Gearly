package com.dominator.gearly.reviews.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Where a review is in moderation, and — the point of this type — <b>which moves between those
 * states are legal</b>.
 *
 * <h2>Why there was no table before</h2>
 * Because there was no rule. {@code AdminReviewService} had one private method,
 * {@code setStatus(id, status)}, that loaded the review and assigned whatever it was handed;
 * {@code approveReview} and {@code rejectReview} were two calls into it with different
 * constants. Nothing anywhere said what a legal moderation move was, so every one of them was:
 * approving an approved review again, rejecting one twice, or — once the rating rollup follows
 * moderation — folding the same stars into a product's average as many times as an
 * administrator happened to click.
 *
 * <p>The same shape as {@code OrderStatus}, for the same reason, and with the same two
 * properties:
 *
 * <ul>
 *   <li><b>Nothing returns to {@code PENDING}.</b> "Not yet moderated" is a fact about history,
 *       not a state a moderator can put a review back into. The plan names this as the bug at
 *       {@code AdminReviewService}: the old code would have accepted it.</li>
 *   <li><b>A status is never a legal source for itself.</b> Re-approving an approved review is
 *       refused with a 409, which is what makes the rollup safe: every arrival at
 *       {@code APPROVED} is a first arrival, so {@code Product.addRating} is called exactly
 *       once per review that counts.</li>
 * </ul>
 *
 * <p>Reconsidering is allowed in both directions — {@code APPROVED → REJECTED} for a review
 * that should not have been published, {@code REJECTED → APPROVED} for one refused in error.
 * Both raise an event, and the catalog undoes or applies the rollup accordingly.
 */
public enum ReviewStatus {
    PENDING,
    APPROVED,
    REJECTED;

    /** Source statuses from which each target may be reached. */
    private static final Map<ReviewStatus, Set<ReviewStatus>> ALLOWED_SOURCES;

    static {
        ALLOWED_SOURCES = new EnumMap<>(ReviewStatus.class);
        // PENDING is absent as a target on purpose: there is no way back to "unmoderated".
        ALLOWED_SOURCES.put(APPROVED, EnumSet.of(PENDING, REJECTED));
        ALLOWED_SOURCES.put(REJECTED, EnumSet.of(PENDING, APPROVED));
    }

    /** Whether a review in this status may move to {@code target}. */
    public boolean canTransitionTo(ReviewStatus target) {
        Set<ReviewStatus> allowedSources = ALLOWED_SOURCES.get(target);
        return allowedSources != null && allowedSources.contains(this);
    }

    /** @throws IllegalReviewTransitionException if {@link #canTransitionTo} says no */
    public void assertCanTransitionTo(ReviewStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalReviewTransitionException(this, target);
        }
    }

    /** Whether a review in this status counts towards its product's rating rollup. */
    public boolean countsTowardsRating() {
        return this == APPROVED;
    }
}
