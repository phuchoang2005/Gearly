package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * The grading of a product's physical condition.
 *
 * <p>Replaces a free-text {@code String} that was compared by exact equality in
 * {@code ProductRepositoryCustomImpl} — meaning a filter value the storefront did not
 * spell identically silently matched nothing, with no error anywhere.
 *
 * <h2>Why the wire value is not the constant name</h2>
 * The stored and transmitted vocabulary is {@code NEW}, {@code LIKE NEW}, {@code GOOD},
 * {@code ACCEPTABLE} — see {@code ConditionFilter.jsx} and {@code ConditionTag.jsx} in the
 * storefront. {@code LIKE NEW} contains a space and so cannot be a Java constant name,
 * which is exactly why this needs an explicit {@link #wireValue} rather than relying on
 * {@code name()}: {@code LIKE_NEW} would have written an underscore into documents that
 * the frontend's colour map and filter set would then both fail to match.
 */
public enum ProductCondition {

    NEW("NEW"),
    LIKE_NEW("LIKE NEW"),
    GOOD("GOOD"),
    ACCEPTABLE("ACCEPTABLE");

    private final String wireValue;

    ProductCondition(String wireValue) {
        this.wireValue = wireValue;
    }

    /** The exact token stored in Mongo and sent on the wire. */
    @JsonValue
    public String wireValue() {
        return wireValue;
    }

    @JsonCreator
    public static ProductCondition fromWireValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return Arrays.stream(values())
                .filter(condition -> condition.wireValue.equalsIgnoreCase(trimmed))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("unknown product condition: " + value));
    }

    @Override
    public String toString() {
        return wireValue;
    }
}
