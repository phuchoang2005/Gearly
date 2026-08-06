package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.AccessDeniedDomainException;

/**
 * Touching somebody else's order. 403.
 *
 * <p>The ordering context's counterpart to {@code ReviewNotYoursException}, and the type behind
 * the sprint's first security item: {@code GET /api/orders/{id}} had <b>no ownership check at
 * all</b>, so any authenticated customer could read any order by guessing an id — delivery
 * address, phone number, payment ledger and all. The gap was documented in
 * {@code OrderQueryService.findById}'s own javadoc rather than closed.
 *
 * <p>The two factories exist because the two refusals had different wire messages before this
 * type existed and both are kept: {@link #toCancel()} carries the exact string
 * {@code CancelOrderService} answered through {@code ApiException(FORBIDDEN, …)}, so closing
 * the read hole changes no response the storefront already handles.
 *
 * <h2>Why 403 and not 404</h2>
 * A 404 would hide whether the order exists, which is the stronger answer. It is not the one
 * taken here: the cancel path has always answered 403 for exactly this case, and making the
 * read path disagree with it — or changing both — is a deliberate behaviour change that
 * belongs in its own commit with the storefront checked, not folded into a security fix.
 */
public class OrderNotYoursException extends AccessDeniedDomainException {

    private OrderNotYoursException(String message) {
        super(message);
    }

    /** Reading an order that is not the caller's. */
    public static OrderNotYoursException toView() {
        return new OrderNotYoursException("You are not allowed to view this order");
    }

    /** Cancelling an order that is not the caller's — the message S8 pinned. */
    public static OrderNotYoursException toCancel() {
        return new OrderNotYoursException("You are not allowed to cancel this order");
    }
}
