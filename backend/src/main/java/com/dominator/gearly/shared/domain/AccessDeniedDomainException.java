package com.dominator.gearly.shared.domain;

/**
 * The caller is authenticated but this is not theirs.
 *
 * <p>The fourth of the shared-kernel bases, alongside {@link DomainConflictException} (409),
 * {@link DomainNotFoundException} (404) and {@link DomainRuleViolationException} (400).
 * {@code GlobalExceptionHandler} maps it to <b>403 Forbidden</b>.
 *
 * <h2>Why the domain needed its own way of saying this</h2>
 * Two places threw {@code new ApiException(HttpStatus.FORBIDDEN, …)} from inside what is now
 * domain or application code — {@code CustomerOrderService} when a customer asked to cancel
 * somebody else's order, and {@code ReviewService} when one asked to review somebody else's.
 * Both named {@code org.springframework.http}, which a domain package may not
 * ({@code domain_is_free_of_framework_types}), and both made the ownership rule a property of
 * the web layer rather than of the aggregate that knows who owns it.
 *
 * <p>The rule is {@code order.isOwnedBy(userId)} now. What it means when the answer is no is
 * this exception; what that becomes over HTTP is the handler's decision, and the response is
 * byte-identical to what the two {@code ApiException}s produced.
 *
 * <h2>Not to be confused with a 401</h2>
 * This is only ever raised for a caller we have already identified. An anonymous request never
 * reaches the code that raises it — the security chain answers that, which is the point of
 * {@code SecurityConfig} pinning the public review reads to {@code GET}.
 */
public abstract class AccessDeniedDomainException extends RuntimeException {

    protected AccessDeniedDomainException(String message) {
        super(message);
    }
}
