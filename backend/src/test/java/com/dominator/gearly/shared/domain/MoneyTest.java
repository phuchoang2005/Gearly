package com.dominator.gearly.shared.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link Money} is the load-bearing value object of the shared kernel: it replaces a
 * {@code double} on six persisted fields, and the whole "no migration, no wire change"
 * premise rests on it round-tripping through a {@code double} without moving a cent.
 */
class MoneyTest {

    @Test
    void normalizesToTwoDecimalPlaces() {
        assertThat(Money.of("1599").amount()).isEqualTo(new BigDecimal("1599.00"));
        assertThat(Money.of("1599.5").amount()).isEqualTo(new BigDecimal("1599.50"));
    }

    @Test
    void roundsHalfUp() {
        assertThat(Money.of("0.125").amount()).isEqualTo(new BigDecimal("0.13"));
        assertThat(Money.of("0.124").amount()).isEqualTo(new BigDecimal("0.12"));
    }

    @Test
    void defaultsToUsd() {
        assertThat(Money.of(10.0).currency()).isEqualTo(Currency.getInstance("USD"));
    }

    @Test
    void equalityIgnoresTheScaleTheAmountArrivedWith() {
        // BigDecimal.equals is scale-sensitive; normalizing in the constructor is what
        // makes Money usable as a map key and in assertions.
        assertThat(Money.of("10")).isEqualTo(Money.of("10.00"));
        assertThat(Money.of("10")).hasSameHashCodeAs(Money.of("10.00"));
    }

    @Nested
    class DoubleRoundTrip {

        /**
         * The premise of S9. Every price in the seed data must survive
         * {@code double -> Money -> double} unchanged, or introducing the type silently
         * rewrites the catalog.
         */
        @Test
        void preservesTheStoredDoubleExactly() {
            double[] storedPrices = {109.99, 120.5, 1599.0, 0.0, 23.45, 3198.0, 15.0, 0.01};
            for (double stored : storedPrices) {
                assertThat(Money.of(stored).toDouble())
                        .as("round-trip of %s", stored)
                        .isEqualTo(stored);
            }
        }

        @Test
        void doesNotDragInTheBinaryExpansionOfTheDouble() {
            // new BigDecimal(109.99) would be 109.9900000000000090949470177292823791503906250
            assertThat(Money.of(109.99).amount()).isEqualTo(new BigDecimal("109.99"));
        }
    }

    @Nested
    class Arithmetic {

        @Test
        void addsAndSubtracts() {
            assertThat(Money.of("10.00").plus(Money.of("5.50"))).isEqualTo(Money.of("15.50"));
            assertThat(Money.of("10.00").minus(Money.of("5.50"))).isEqualTo(Money.of("4.50"));
        }

        @Test
        void multipliesByAQuantityAndByARate() {
            assertThat(Money.of("1599.00").times(2)).isEqualTo(Money.of("3198.00"));
            assertThat(Money.of("1599.00").times(new Quantity(2))).isEqualTo(Money.of("3198.00"));
            // the 8% tax rule from CustomerOrderService, unchanged
            assertThat(Money.of("100.00").times(new BigDecimal("0.08"))).isEqualTo(Money.of("8.00"));
        }

        @Test
        void reproducesTheExistingOrderTotalArithmetic() {
            // subtotal 2 x 1599.00, 8% tax, free shipping above the 30.00 threshold
            Money subtotal = Money.of("1599.00").times(2);
            Money tax = subtotal.times(new BigDecimal("0.08"));
            Money total = subtotal.plus(tax);

            assertThat(subtotal.isGreaterThan(Money.of("30.00"))).isTrue();
            assertThat(total).isEqualTo(Money.of("3453.84"));
            assertThat(total.toDouble()).isEqualTo(3453.84);
        }

        @Test
        void comparesAndReportsSign() {
            assertThat(Money.of("10.00").isGreaterThan(Money.of("9.99"))).isTrue();
            assertThat(Money.of("10.00").isLessThan(Money.of("10.01"))).isTrue();
            assertThat(Money.ZERO.isZero()).isTrue();
            assertThat(Money.of("-1.00").isNegative()).isTrue();
        }

        @Test
        void refusesToMixCurrencies() {
            Money vnd = new Money(new BigDecimal("100"), Currency.getInstance("VND"));
            assertThatThrownBy(() -> Money.of("10.00").plus(vnd))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("USD")
                    .hasMessageContaining("VND");
        }
    }

    @Test
    void rejectsNulls() {
        assertThatThrownBy(() -> new Money(null, Money.USD))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class);
    }
}
