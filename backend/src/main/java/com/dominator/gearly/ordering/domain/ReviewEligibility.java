package com.dominator.gearly.ordering.domain;

/**
 * Ordering's answer to "may this person review this order?".
 *
 * <p>An enum because it has to be: it is the return type of {@link ReviewableOrders}, a port
 * another context consumes, and ArchUnit's
 * {@code contexts_touch_each_other_only_through_published_types} recognizes an enum in a
 * {@code domain} package as published language. A record carrying the reasons would not be —
 * and an {@code Order} certainly is not, which is the whole reason this type exists rather than
 * the reviews context loading the aggregate and asking it three questions.
 *
 * <p>Each answer names one fact about the order. Turning a fact into an HTTP status is the
 * caller's job, and the reviews context does it in one place.
 */
public enum ReviewEligibility {

    /** The order exists, belongs to the caller, has been delivered, and has not been reviewed. */
    ELIGIBLE,

    /** No order with that id. */
    NO_SUCH_ORDER,

    /** Somebody else's order. */
    NOT_THE_BUYERS,

    /**
     * The order has not reached a state where there is anything to have an opinion about.
     *
     * <p>This is the S8 {@code KNOWN BUG} the plan calls a missing reviewable-status rule:
     * {@code createReview} asked only who owned the order, so a {@code CANCELLED} order — one
     * whose goods were never sent — could be reviewed, and so could a {@code PENDING} one that
     * had not been paid for.
     */
    NOT_YET_DELIVERED,

    /**
     * Already reviewed.
     *
     * <p>The other S8 {@code KNOWN BUG}: {@code createReview} wrote {@code order.markReviewed()}
     * and never read the flag back, so calling it twice folded the same stars into the product's
     * running total twice. The flag was being maintained for nobody.
     */
    ALREADY_REVIEWED;

    public boolean isEligible() {
        return this == ELIGIBLE;
    }
}
