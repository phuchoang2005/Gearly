package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;

/**
 * A submitted profile value the shared kernel's value objects will not accept — a malformed
 * email address or phone number. Answers 400.
 *
 * <p>The backstop, not the primary guard: the request DTOs carry bean validation, so a real
 * request is refused at the edge with a field-level message. This catches the paths that reach
 * a use case without passing through one — and turns {@code EmailAddress.of}'s
 * {@link IllegalArgumentException}, which would otherwise be an unmapped 500, into the 400 it
 * always was.
 */
public class ProfileValueRejectedException extends DomainRuleViolationException {

    public ProfileValueRejectedException(String message) {
        super(message);
    }
}
