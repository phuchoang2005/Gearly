package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The identity of a customer order, as a type the compiler can tell apart from every other id in
 * the system.
 *
 * <p>Stored and serialized as the same bare string it has always been; see {@link DomainId}.
 */
public record OrderId(String value) implements DomainId {

    public OrderId {
        value = DomainId.requireId(value, "order id");
    }

    @JsonCreator
    public static OrderId of(String value) {
        return new OrderId(value);
    }

    @JsonValue
    @Override
    public String toString() {
        return value;
    }
}
