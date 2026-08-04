package com.dominator.gearly.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    void rejectsANegativeQuantity() {
        assertThatThrownBy(() -> new Quantity(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void allowsZero() {
        // an out-of-stock product is a legitimate state; a negative one is not
        assertThat(Quantity.ZERO.value()).isZero();
        assertThat(Quantity.ZERO.isZero()).isTrue();
    }

    @Test
    void addsAndSubtracts() {
        assertThat(Quantity.of(3).plus(Quantity.of(2))).isEqualTo(Quantity.of(5));
        assertThat(Quantity.of(3).minus(Quantity.of(2))).isEqualTo(Quantity.ONE);
    }

    /**
     * The oversell guard. Reserving more than is on hand is not a special case handled by
     * a caller's {@code if} — it cannot be represented at all.
     */
    @Test
    void subtractingMoreThanIsAvailableIsNotRepresentable() {
        assertThatThrownBy(() -> Quantity.of(1).minus(Quantity.of(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be negative");
    }

    @Test
    void comparesStockAgainstDemand() {
        assertThat(Quantity.of(5).isAtLeast(Quantity.of(5))).isTrue();
        assertThat(Quantity.of(4).isAtLeast(Quantity.of(5))).isFalse();
        assertThat(Quantity.of(4).isLessThan(Quantity.of(5))).isTrue();
    }

    @Test
    void serializesAsABareInt() {
        assertThat(Quantity.of(7).toInt()).isEqualTo(7);
    }
}
