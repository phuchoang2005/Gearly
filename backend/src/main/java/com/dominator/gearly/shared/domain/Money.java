package com.dominator.gearly.shared.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * A monetary amount in a single currency, always carried at scale 2 with
 * {@link RoundingMode#HALF_UP}.
 *
 * <p>This type exists to end a specific bug class in this codebase: money was a
 * {@code double} on every persisted field but a {@link BigDecimal} in the calculation
 * layer, round-tripped lossily on the way back out (the old
 * {@code order.setTotalAmount(grandTotalUsd.doubleValue())}). Arithmetic now happens in
 * {@code BigDecimal} from end to end and the {@code double} exists only at the two edges
 * where the outside world demands one.
 *
 * <h2>Why it still serializes as a double</h2>
 * {@link #toDouble()} is the {@code @JsonValue}, and the Mongo write converter emits a
 * {@code double} as well. That is deliberate and load-bearing: the stored BSON type and
 * the JSON on the wire are byte-identical to what they were before this type existed, so
 * introducing it needed no data migration and no frontend change. A {@code BigDecimal}
 * {@code @JsonValue} would have written {@code 1599.00} where the wire has {@code 1599.0}
 * — a different JSON token, and a visible break.
 *
 * <p><b>The currency is not persisted.</b> Every price in this system is USD (MoMo
 * converts to VND at the gateway edge, see {@code FxService}), so the stored document
 * keeps a bare number and {@link #of(double)} reconstitutes it as USD. Mixing currencies
 * in arithmetic throws rather than silently coercing.
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    /** Money is stored and displayed to the cent. */
    public static final int SCALE = 2;

    /** The system currency. See the class note on why it is not persisted. */
    public static final Currency USD = Currency.getInstance("USD");

    public static final Money ZERO = Money.of(BigDecimal.ZERO);

    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, USD);
    }

    /**
     * Rebuilds a {@code Money} from the {@code double} that Mongo and the wire carry.
     *
     * <p>Uses {@link BigDecimal#valueOf(double)} — i.e. the {@code Double.toString}
     * representation — not {@code new BigDecimal(double)}, which would drag in the full
     * binary expansion and turn {@code 109.99} into {@code 109.9900000000000090949...}
     * before rounding.
     */
    @JsonCreator
    public static Money of(double amount) {
        return of(BigDecimal.valueOf(amount));
    }

    /** Parses a decimal string, e.g. from configuration. */
    public static Money of(String amount) {
        return of(new BigDecimal(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(requireSameCurrency(other).amount), currency);
    }

    public Money minus(Money other) {
        return new Money(amount.subtract(requireSameCurrency(other).amount), currency);
    }

    /** Scales the amount, e.g. a line price by its quantity or a subtotal by a tax rate. */
    public Money times(BigDecimal multiplier) {
        return new Money(amount.multiply(multiplier), currency);
    }

    public Money times(int multiplier) {
        return times(BigDecimal.valueOf(multiplier));
    }

    public Money times(Quantity quantity) {
        return times(quantity.value());
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    /**
     * The {@code double} the wire and the database see. See the class note — this is the
     * compatibility seam, not a convenience.
     */
    @JsonValue
    public double toDouble() {
        return amount.doubleValue();
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(requireSameCurrency(other).amount);
    }

    @Override
    public String toString() {
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }

    private Money requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other must not be null");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "cannot combine " + currency.getCurrencyCode() + " with " + other.currency.getCurrencyCode());
        }
        return other;
    }
}
