package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.DomainNotFoundException;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.UserId;

/** No such account. Mapped centrally to 404 — see {@code GlobalExceptionHandler}. */
public class UserNotFoundException extends DomainNotFoundException {

    public UserNotFoundException(UserId userId) {
        super("User not found: " + userId);
    }

    public UserNotFoundException(String message) {
        super(message);
    }

    public static UserNotFoundException withEmail(EmailAddress email) {
        return new UserNotFoundException("Email not registered.");
    }
}
