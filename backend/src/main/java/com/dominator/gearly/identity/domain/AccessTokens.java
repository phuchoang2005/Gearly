package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.EmailAddress;

import java.util.Optional;

/**
 * Issuing and reading the bearer token a signed-in client presents. A port, for the same
 * reason as {@link PasswordHasher}: JWT is a technology choice, and the context that owns
 * sign-in should not be able to name one.
 *
 * <h2>{@link #subjectOf} returns an {@link Optional}, and that is a security fix</h2>
 * {@code JwtUtil} exposed {@code extractEmail} and {@code validateToken} as two calls, and
 * {@code JwtAuthenticationFilter} made them in the wrong order — it extracted first and
 * validated second, so an expired or tampered token threw out of the parser before anything
 * checked it. An exception from inside a servlet filter is not an authentication failure the
 * security chain can turn into a 401; it propagates, and the caller got a <b>500</b>. Anyone
 * with an expired session saw a server error instead of being asked to sign in again.
 *
 * <p>Splitting the two calls is what made the mistake possible, so the port does not offer
 * them separately. There is one question — "whose token is this, if anyone's" — and an
 * unreadable, unsigned, or expired token is simply an empty answer.
 */
public interface AccessTokens {

    /** A fresh token identifying {@code subject}. */
    String issueFor(EmailAddress subject);

    /**
     * Who this token identifies.
     *
     * @return empty if the token is malformed, unsigned, signed by someone else, or expired
     */
    Optional<EmailAddress> subjectOf(String token);
}
