package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A non-negative count of units — cart lines, order lines, stock on hand.
 *
 * <p>Zero is legal (an out-of-stock product holds {@code Quantity.ZERO}); negative is
 * not, which is the invariant the five duplicated stock checks are each trying to
 * express by hand today. S11 collapses them onto {@code Product.reserve(Quantity)}.
 *
 * <p>Serializes as a bare {@code int} on both the wire and in Mongo, so introducing it
 * changes neither.
 */
public record Quantity(int value) implements Comparable<Quantity> {

    public static final Quantity ZERO = new Quantity(0);
    public static final Quantity ONE = new Quantity(1);

    public Quantity {
        if (value < 0) {
            throw new IllegalArgumentException("quantity must not be negative, was " + value);
        }
    }

    @JsonCreator
    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity plus(Quantity other) {
        return new Quantity(value + other.value);
    }

    /**
     * @throws IllegalArgumentException if {@code other} exceeds this quantity — the
     *         constructor's non-negative invariant is what rejects an oversell.
     */
    public Quantity minus(Quantity other) {
        return new Quantity(value - other.value);
    }

    public boolean isZero() {
        return value == 0;
    }

    public boolean isAtLeast(Quantity other) {
        return value >= other.value;
    }

    public boolean isLessThan(Quantity other) {
        return value < other.value;
    }

    @JsonValue
    public int toInt() {
        return value;
    }

    @Override
    public int compareTo(Quantity other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
