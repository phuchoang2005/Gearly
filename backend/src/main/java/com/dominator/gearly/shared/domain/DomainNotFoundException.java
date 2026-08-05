package com.dominator.gearly.shared.domain;

/**
 * The aggregate the request names does not exist.
 *
 * <p>The sibling of {@link DomainConflictException}, and there for the same reason: a domain
 * package may not import {@code exception.ResourceNotFoundException}, which carries an
 * {@code org.springframework.http.HttpStatus} and lives in one of the pre-refactor packages
 * ArchUnit's {@code domain_does_not_reach_back_into_legacy_packages} bans a domain from
 * reaching into. The domain states what is missing; {@code GlobalExceptionHandler} turns that
 * into <b>404 Not Found</b>, which is the same response {@code ResourceNotFoundException}
 * already produced.
 *
 * <p>One mapping covers every context, however many subclasses S12 and S13 add.
 */
public abstract class DomainNotFoundException extends RuntimeException {

    protected DomainNotFoundException(String message) {
        super(message);
    }
}
