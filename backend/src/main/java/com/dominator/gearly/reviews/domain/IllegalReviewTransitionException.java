package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.DomainConflictException;

/**
 * A moderation move the lifecycle does not allow — approving an already-approved review, or
 * putting a moderated one back to {@code PENDING}. 409, via
 * {@code GlobalExceptionHandler}'s single {@link DomainConflictException} mapping.
 */
public class IllegalReviewTransitionException extends DomainConflictException {

    public IllegalReviewTransitionException(ReviewStatus from, ReviewStatus to) {
        super("A review that is " + from + " cannot become " + to + ".");
    }
}
