package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.DomainConflictException;

/**
 * The verification link has already been used. 409, as {@code ConflictException} returned
 * before — but stated by the aggregate, which is the only thing that knows.
 */
public class AccountAlreadyVerifiedException extends DomainConflictException {

    public AccountAlreadyVerifiedException() {
        super("Account already verified.");
    }
}
