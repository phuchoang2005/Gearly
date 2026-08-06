package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.AccessDeniedDomainException;

/**
 * Reviewing somebody else's purchase. 403.
 *
 * <p>Replaces {@code throw new ApiException(HttpStatus.FORBIDDEN, …)} inside what is now
 * application code — one of the two places the plan names. Same status, same message; what
 * changed is that the reviews context states the rule without naming
 * {@code org.springframework.http}, which {@code domain_is_free_of_framework_types} forbids it
 * from doing.
 */
public class ReviewNotYoursException extends AccessDeniedDomainException {

    public ReviewNotYoursException() {
        super("You are not allowed to review the items in this order");
    }
}
