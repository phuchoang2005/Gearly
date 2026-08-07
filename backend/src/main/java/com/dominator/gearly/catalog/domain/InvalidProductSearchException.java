package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;

/**
 * A product-search query parameter the catalog cannot interpret. Answers 400.
 *
 * <p>Both cases were S9 and S11 fixes and are preserved exactly: an unrecognised
 * {@code condition} used to be a string-equality match that could only ever return nothing, and
 * an unrecognised {@code genres} value reached {@code new ObjectId(...)} inside the repository
 * and surfaced as a 500.
 */
public class InvalidProductSearchException extends DomainRuleViolationException {

    public InvalidProductSearchException(String message) {
        super(message);
    }
}
