package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;

/**
 * A {@code ?status=} value that is not an {@link OrderStatus}. Answers 400.
 *
 * <p>S10 made this a 400; before that the same request was a 500 or an empty list depending on
 * whether a search term accompanied it, because the two query paths read the parameter
 * differently. This only changes which class says so.
 */
public class UnknownOrderStatusException extends DomainRuleViolationException {

    public UnknownOrderStatusException(String status) {
        super("Unknown order status: " + status);
    }
}
