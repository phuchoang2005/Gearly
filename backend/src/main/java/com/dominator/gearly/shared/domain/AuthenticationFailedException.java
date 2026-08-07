package com.dominator.gearly.shared.domain;

/**
 * The caller is not who they claim to be, or cannot be let in at all — answered <b>401</b>.
 *
 * <p>The fifth and last of the shared kernel's exception bases, joining
 * {@link DomainRuleViolationException} (400), {@link AccessDeniedDomainException} (403),
 * {@link DomainNotFoundException} (404) and {@link DomainConflictException} (409). It exists
 * because S13 had to give the last legacy exception — {@code exception.UnauthorizedException} —
 * somewhere to go that a bounded context is allowed to name.
 *
 * <p><b>401, not 403.</b> The distinction is the one the HTTP specs draw and it is worth
 * keeping: this says "I do not know who you are", where {@link AccessDeniedDomainException}
 * says "I know who you are and you may not". Sign-in refusals are the former.
 *
 * <p>Abstract on purpose, like its four siblings. A context names its own refusal so the reason
 * is in the type rather than only in a string, and so that the mapping to a status code stays a
 * decision made once, in {@code platform.exception.GlobalExceptionHandler}, rather than at every
 * throw site.
 */
public abstract class AuthenticationFailedException extends RuntimeException {

    protected AuthenticationFailedException(String message) {
        super(message);
    }
}
