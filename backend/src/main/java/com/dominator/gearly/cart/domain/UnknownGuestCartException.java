package com.dominator.gearly.cart.domain;

import com.dominator.gearly.shared.domain.AccessDeniedDomainException;

/**
 * A guest cart id this server did not issue. 403.
 *
 * <p>Raised by the guest-cart routes when {@link GuestCartIds#verify} comes back empty — a
 * forged signature, a value from another deployment, or a bare UUID stored by a browser from
 * before ids were signed.
 *
 * <p><b>The storefront is expected to recover from this, not to show it.</b> The last case is
 * ordinary: a returning visitor holding an old {@code localStorage.guestId} meets it exactly
 * once. {@code useCartData} drops the stored id and re-inits on a 403, which loses whatever was
 * in that basket — the alternative was accepting unsigned ids indefinitely, which is the hole
 * this closes.
 */
public class UnknownGuestCartException extends AccessDeniedDomainException {

    public UnknownGuestCartException() {
        super("Unknown guest cart. Start a new one.");
    }
}
