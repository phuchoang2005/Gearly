package com.dominator.gearly.shared.domain;

/**
 * The request asks for something the domain cannot give: ten units of a product that has three,
 * a quantity below one, a cart that claims both an owner and a guest.
 *
 * <p>The third of the shared-kernel bases, alongside {@link DomainConflictException} (409) and
 * {@link DomainNotFoundException} (404). {@code GlobalExceptionHandler} maps this one to
 * <b>400 Bad Request</b>.
 *
 * <h2>Why 400 and not 409</h2>
 * There is a reading on which an oversell is a conflict — the request was fine when it was
 * composed and the world moved underneath it. But 400 is what {@code BadRequestException}
 * returns today from all five of the stock checks S11 collapses, and the S8 characterization
 * suite pins it. Changing the rule's <em>location</em> and changing its <em>status code</em>
 * are two separate decisions, and this sprint is only making the first one.
 */
public abstract class DomainRuleViolationException extends RuntimeException {

    protected DomainRuleViolationException(String message) {
        super(message);
    }
}
