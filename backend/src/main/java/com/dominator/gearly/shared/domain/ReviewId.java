package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The identity of a product review, as a type the compiler can tell apart from every other id in
 * the system.
 *
 * <p>Stored and serialized as the same bare string it has always been; see {@link DomainId}.
 */
public record ReviewId(String value) implements DomainId {

    public ReviewId {
        value = DomainId.requireId(value, "review id");
    }

    @JsonCreator
    public static ReviewId of(String value) {
        return new ReviewId(value);
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
