package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The identity of a user account, as a type the compiler can tell apart from every other id in
 * the system.
 *
 * <p>Stored and serialized as the same bare string it has always been; see {@link DomainId}.
 */
public record UserId(String value) implements DomainId {

    public UserId {
        value = DomainId.requireId(value, "user id");
    }

    @JsonCreator
    public static UserId of(String value) {
        return new UserId(value);
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
