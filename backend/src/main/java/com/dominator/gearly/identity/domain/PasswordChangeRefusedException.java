package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;

/** A password change or reset this system will not perform. Answers 400. */
public class PasswordChangeRefusedException extends DomainRuleViolationException {

    private PasswordChangeRefusedException(String message) {
        super(message);
    }

    public static PasswordChangeRefusedException emailNotVerified() {
        return new PasswordChangeRefusedException(
                "Please verify your email before resetting password.");
    }

    public static PasswordChangeRefusedException oldPasswordDoesNotMatch() {
        return new PasswordChangeRefusedException(
                "Old password does not match with your current password.");
    }
}
