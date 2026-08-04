package com.dominator.gearly.shared.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * The contract shared by every typed identifier in the kernel.
 *
 * <p>Typed ids exist here for one reason: {@code String} ids are mutually assignable, so
 * the compiler cannot tell {@code productId} from {@code orderId} from {@code userId}.
 * That is not hypothetical in this codebase — {@code ReviewService} juggles three
 * {@code ObjectId}s and a {@code String} user id in the same method, and
 * {@code applyStockAndClearCart} keys a map of quantities by a product id string that a
 * user id would fit into just as well.
 *
 * <p>Every implementation is a {@code record} over a {@code String} and serializes as a
 * bare string, so the wire and the stored document are unchanged. The one exception is
 * {@link CategoryId}, which is stored as a BSON {@code ObjectId} — see its javadoc.
 */
public interface DomainId {

    /** 24 hex characters — the string form of a Mongo {@code ObjectId}. */
    Pattern OBJECT_ID = Pattern.compile("^[0-9a-fA-F]{24}$");

    String value();

    static String requireId(String value, String what) {
        Objects.requireNonNull(value, what + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(what + " must not be blank");
        }
        return trimmed;
    }

    static String requireObjectId(String value, String what) {
        String trimmed = requireId(value, what);
        if (!OBJECT_ID.matcher(trimmed).matches()) {
            throw new IllegalArgumentException(what + " must be a 24-character hex ObjectId, was: " + value);
        }
        return trimmed;
    }
}
