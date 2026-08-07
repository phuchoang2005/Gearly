package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.AuthenticationFailedException;

/**
 * A sign-in this system will not grant. Answers 401.
 *
 * <p>One type with four named reasons rather than four types, because the reasons differ only
 * in what the customer is told — every one of them means "no session for you" and every one
 * answers the same status. Where the reason changes what a <em>caller</em> should do, it gets
 * its own type; none of these do.
 *
 * <p><b>The messages are deliberately uneven, and that is correct.</b>
 * {@link #invalidCredentials()} says nothing about whether the account exists — the same text
 * for an unknown address and a wrong password, so the endpoint cannot be used to enumerate
 * customers. The other three describe a real, actionable state of a known account, which the
 * caller has already proven they can reach.
 */
public class SignInRefusedException extends AuthenticationFailedException {

    private SignInRefusedException(String message) {
        super(message);
    }

    /** Wrong password, or no such account — deliberately indistinguishable. */
    public static SignInRefusedException invalidCredentials() {
        return new SignInRefusedException("Invalid credentials");
    }

    public static SignInRefusedException emailNotVerified() {
        return new SignInRefusedException("Please verify your email before logging in.");
    }

    public static SignInRefusedException accountInactive() {
        return new SignInRefusedException("This account had been set to inactive. \n"
                + "Please contact Gearly Support if you need to activate your account.");
    }

    public static SignInRefusedException invalidGoogleToken() {
        return new SignInRefusedException("Invalid Google ID-token");
    }
}
