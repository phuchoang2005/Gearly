package com.dominator.gearly.platform.security;

import com.dominator.gearly.cart.domain.GuestCartIds;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * {@link GuestCartIds} over HMAC-SHA256. The crypto adapter for the cart context's port, sitting
 * alongside {@link JwtAccessTokens} and {@link BCryptPasswordHasher} for the same reason: a
 * bounded context may not name a cryptographic type, so the platform holds them all.
 *
 * <h2>The format</h2>
 * {@code <uuid>.<base64url(hmacSha256(uuid))>} — the signature is truncation-free and the
 * separator cannot occur in either half, so parsing is unambiguous. It is not a JWT: there are
 * no claims, no expiry and nothing to negotiate. A guest cart's lifetime is the TTL index's
 * business, not the id's, and adding an expiry here would silently orphan baskets the TTL still
 * considers live.
 *
 * <h2>The key</h2>
 * {@code jwt.secret}, deliberately reused rather than given its own property. A second secret is
 * a second thing to configure, rotate and forget in a deployment, and the two uses are already
 * bound to the same trust boundary — anyone who can forge a guest id from this key can mint an
 * admin JWT from it, so a separate key protects nothing that is not already lost.
 *
 * <p>Rotating it invalidates outstanding guest ids, which is a re-init on the storefront's next
 * call, not a lost cart for anyone signed in.
 */
@Component
public class HmacGuestCartIds implements GuestCartIds {

    private static final String ALGORITHM = "HmacSHA256";
    private static final char SEPARATOR = '.';

    private final SecretKeySpec key;

    public HmacGuestCartIds(@Value("${jwt.secret}") String secret) {
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
    }

    @Override
    public String issue() {
        String id = UUID.randomUUID().toString();
        return id + SEPARATOR + sign(id);
    }

    @Override
    public Optional<String> verify(String presented) {
        if (presented == null) {
            return Optional.empty();
        }
        int split = presented.lastIndexOf(SEPARATOR);
        if (split < 1 || split == presented.length() - 1) {
            return Optional.empty();
        }

        String id = presented.substring(0, split);
        String signature = presented.substring(split + 1);

        // Constant-time: a length-independent comparison here would leak the signature one
        // character at a time to a caller willing to time the responses.
        if (!MessageDigest.isEqual(signature.getBytes(StandardCharsets.UTF_8),
                sign(id).getBytes(StandardCharsets.UTF_8))) {
            return Optional.empty();
        }
        return Optional.of(id);
    }

    private String sign(String id) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(id.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException impossible) {
            // HmacSHA256 is required of every JRE, and the key is non-empty by construction.
            throw new IllegalStateException("HMAC-SHA256 unavailable", impossible);
        }
    }
}
