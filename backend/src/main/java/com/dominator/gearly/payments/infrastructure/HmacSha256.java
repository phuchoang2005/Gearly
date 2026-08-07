package com.dominator.gearly.payments.infrastructure;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * HMAC-SHA256, hex-encoded — the single copy.
 *
 * <p>There were two, byte-for-byte identical, in {@code MomoService} and
 * {@code PaymentController}. Duplicated crypto is worse than duplicated anything else: the two
 * copies only have to disagree once, and the failure mode is a signature check that passes when
 * it should not. It is also how the two ended up with different comparison semantics, which is
 * the bug below.
 *
 * <h2>Why {@link #matches} is not {@code equals}</h2>
 * {@code PaymentController.verifySignature} compared the expected and received digests with
 * {@code String.equals}, which returns as soon as two characters differ. The time it takes is
 * therefore a function of how long a common prefix the attacker supplied, and an attacker who
 * can measure that can recover a valid signature one hex digit at a time without ever knowing
 * the key. {@link MessageDigest#isEqual(byte[], byte[])} is the constant-time comparison; the
 * inputs are hex here, so the comparison is over their bytes.
 *
 * <p>Whether the timing signal survives the network is not the question worth arguing — the
 * constant-time compare costs nothing, and "probably too noisy to exploit" is not a security
 * property.
 */
final class HmacSha256 {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacSha256() {
    }

    /** The lower-case hex digest of {@code data} under {@code key}. */
    static String hexDigest(String data, String key) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            // A missing HmacSHA256 or a malformed key is a deployment fault, not a bad request.
            throw new IllegalStateException("HMAC-SHA256 is unavailable or the key is unusable", e);
        }
    }

    /**
     * Whether {@code received} is the digest of {@code data} under {@code key}, compared in
     * time independent of how much of it is correct.
     *
     * @param received the signature as presented by the caller; {@code null} is never a match
     */
    static boolean matches(String data, String key, String received) {
        if (received == null) {
            return false;
        }
        return MessageDigest.isEqual(
                hexDigest(data, key).getBytes(StandardCharsets.UTF_8),
                received.getBytes(StandardCharsets.UTF_8));
    }
}
