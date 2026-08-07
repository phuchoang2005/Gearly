package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;

/**
 * A star count outside 1–5 reaching the use case. Answers 400.
 *
 * <p>The backstop behind {@code CreateReviewRequestDTO}'s {@code @Min(1) @Max(5)}, for any path
 * that does not pass bean validation. Without it, {@code Rating.of}'s
 * {@link IllegalArgumentException} would be an unmapped 500 — and before S11 there was no bound
 * at all, which is how the S8 suite came to pin a rating of 900 being folded into a product's
 * average.
 */
public class RatingOutOfRangeException extends DomainRuleViolationException {

    public RatingOutOfRangeException(String message) {
        super(message);
    }
}
