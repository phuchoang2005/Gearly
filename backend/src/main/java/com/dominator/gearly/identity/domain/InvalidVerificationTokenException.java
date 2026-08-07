package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;

/**
 * A verification or password-reset token that cannot be acted on. Answers 400.
 *
 * <p>{@link #invalid()} covers both "no such token" and "already used", for the same reason
 * {@code SignInRefusedException.invalidCredentials} is vague: a caller holding a guessed token
 * should not learn whether they guessed a real one.
 */
public class InvalidVerificationTokenException extends DomainRuleViolationException {

    private InvalidVerificationTokenException(String message) {
        super(message);
    }

    public static InvalidVerificationTokenException invalid() {
        return new InvalidVerificationTokenException("Invalid or expired token");
    }

    /** Distinguished from {@link #invalid()} because it is a token the holder legitimately had. */
    public static InvalidVerificationTokenException expired() {
        return new InvalidVerificationTokenException("Token expired!");
    }
}
