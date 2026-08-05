package com.dominator.gearly.shared.domain;

/**
 * A domain rule refuses the requested state change: the request was well-formed and the
 * caller was allowed to make it, but the aggregate is not in a state where it makes sense.
 * An illegal order-status transition and cancelling an already-shipped order are both this.
 *
 * <p>It lives in the shared kernel, not in a context, so that {@code GlobalExceptionHandler}
 * needs exactly one mapping — <b>409 Conflict</b> — however many contexts grow subclasses of
 * it in S11–S13.
 *
 * <h2>Why not reuse {@code exception.ConflictException}</h2>
 * Because the domain may not import it. {@code exception/} is one of the pre-refactor
 * packages ArchUnit's {@code domain_does_not_reach_back_into_legacy_packages} bans a domain
 * package from reaching into, and {@code ApiException} additionally carries a
 * {@code org.springframework.http.HttpStatus} — a web type the domain must not name at all.
 * An aggregate states the rule that was broken; choosing a status code is the web edge's job.
 * The HTTP response is identical either way.
 */
public abstract class DomainConflictException extends RuntimeException {

    protected DomainConflictException(String message) {
        super(message);
    }
}
