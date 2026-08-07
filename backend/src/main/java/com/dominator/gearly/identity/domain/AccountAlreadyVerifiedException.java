package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.DomainConflictException;

/** Re-verifying an account that is already verified. Answers 409, as it always did. */
public class AccountAlreadyVerifiedException extends DomainConflictException {

    public AccountAlreadyVerifiedException() {
        super("User already verified.");
    }
}
