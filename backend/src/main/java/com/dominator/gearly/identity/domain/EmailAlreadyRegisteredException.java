package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.DomainConflictException;

/**
 * That address already has an account. 409, and the message is the one registration has
 * always returned.
 */
public class EmailAlreadyRegisteredException extends DomainConflictException {

    public EmailAlreadyRegisteredException() {
        super("Email already registered.");
    }
}
