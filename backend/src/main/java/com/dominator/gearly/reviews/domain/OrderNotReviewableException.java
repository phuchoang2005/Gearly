package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.ordering.domain.ReviewEligibility;
import com.dominator.gearly.shared.domain.DomainRuleViolationException;

/**
 * The order named cannot be reviewed — it has not been delivered, or it already has been.
 *
 * <p>400, which is what {@code BadRequestException} would have returned had either rule
 * existed. The two cases it covers are the S8 characterization suite's remaining
 * {@code KNOWN BUG}s, and the phrasing comes from {@link ReviewEligibility} so that the reason
 * given is the reason ordering actually gave.
 *
 * <p>{@code NOT_THE_BUYERS} deliberately does not arrive here: that is an access decision, not
 * a rule about the order, and it is raised as {@code ReviewNotYoursException} so it answers 403.
 */
public class OrderNotReviewableException extends DomainRuleViolationException {

    public OrderNotReviewableException(ReviewEligibility eligibility) {
        super(messageFor(eligibility));
    }

    private static String messageFor(ReviewEligibility eligibility) {
        return switch (eligibility) {
            case ALREADY_REVIEWED -> "You have already reviewed this order.";
            case NOT_YET_DELIVERED -> "You can only review an order once it has been delivered.";
            default -> "This order cannot be reviewed.";
        };
    }
}
