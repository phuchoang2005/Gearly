package com.dominator.gearly.cart.domain;

import java.util.Optional;

/**
 * Issues and checks the id a not-signed-in visitor's basket is keyed by.
 *
 * <h2>The hole this closes</h2>
 * {@code /api/guest-cart/**} is {@code permitAll} and every route took a {@code guestId}
 * straight from the query string with <b>no binding of any kind</b>. Two consequences, both
 * real:
 *
 * <ul>
 *   <li><b>Anyone who learns a UUID owns that basket</b> — can read it, add to it, empty it.
 *       A guest id travels in a URL, so it reaches browser history, referrer headers and access
 *       logs.</li>
 *   <li><b>Any string at all created a cart.</b> {@code CartService.getOrCreate} inserts on
 *       miss, so a loop over arbitrary ids filled the collection with documents that nothing
 *       would ever read. The TTL index eventually reclaims them; nothing stopped them being
 *       written.</li>
 * </ul>
 *
 * <p>The fix is that the server only ever accepts an id it issued: {@link #issue()} hands out a
 * signed value and {@link #verify} refuses anything it did not sign. It is not authentication —
 * a guest has no account — it is the weaker property that actually matters here, that the id is
 * unguessable <em>and</em> unforgeable rather than merely long.
 *
 * <h2>Why this is a port</h2>
 * Signing is cryptography and cryptography is the platform's, exactly as password hashing and
 * JWTs are: {@code platform.security.HmacGuestCartIds} implements this the way
 * {@code JwtAccessTokens} implements {@code identity.domain.AccessTokens}. The cart context
 * states what it needs and never names a crypto type.
 */
public interface GuestCartIds {

    /**
     * A fresh, signed id for a visitor who does not have one.
     *
     * <p>Opaque to the client, which stores it and echoes it back. The storefront already
     * treats it that way, which is why the format change needs no code change there.
     */
    String issue();

    /**
     * The raw id inside a presented value, if this server signed it.
     *
     * <p>Empty for anything else — a forged signature, a bare UUID from before this existed, a
     * value from a different deployment. Returning the unwrapped id rather than the whole token
     * is what keeps the stored document unchanged: {@code carts.guestId} holds the same UUID it
     * always did.
     */
    Optional<String> verify(String presented);
}
