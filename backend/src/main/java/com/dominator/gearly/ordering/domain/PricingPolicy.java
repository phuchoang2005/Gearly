package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.Money;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * What an order costs: the tax rate, and the threshold above which shipping is free.
 *
 * <p>These four numbers were {@code private static final} fields on
 * {@code CustomerOrderService}, which made them invisible to the two admin write paths that
 * also compute a total — and those two disagreed both with it and with each other.
 * {@code PATCH} re-derived the total as the bare sum of the submitted lines, silently
 * dropping the tax and shipping the order was placed with; {@code PUT} took whatever
 * {@code totalAmount} the request body carried, so an admin could send lines worth $10 and a
 * total of $10,000 and the order would store both. With one policy object, every path that
 * changes an order's lines re-derives the total the same way.
 *
 * <h2>Behavior preserved exactly</h2>
 * 8% tax on the line subtotal; a subtotal <em>strictly greater than</em> $30 ships free,
 * otherwise shipping is $15. The boundary matters and is pinned by the S8 characterization
 * suite: a subtotal of exactly $30.00 still pays for shipping.
 *
 * <p>Deliberately a plain object with no Spring annotations — the domain may not carry them.
 * It is constructed from {@code @ConfigurationProperties} by {@code OrderingConfiguration}
 * in {@code ordering.infrastructure}.
 */
public class PricingPolicy {

    private final BigDecimal taxRate;
    private final Money freeShippingThreshold;
    private final Money standardShippingCost;

    public PricingPolicy(BigDecimal taxRate, Money freeShippingThreshold, Money standardShippingCost) {
        this.taxRate = Objects.requireNonNull(taxRate, "taxRate must not be null");
        this.freeShippingThreshold = Objects.requireNonNull(freeShippingThreshold, "freeShippingThreshold must not be null");
        this.standardShippingCost = Objects.requireNonNull(standardShippingCost, "standardShippingCost must not be null");
        if (taxRate.signum() < 0) {
            throw new IllegalArgumentException("taxRate must not be negative, was " + taxRate);
        }
        if (standardShippingCost.isNegative()) {
            throw new IllegalArgumentException("standardShippingCost must not be negative, was " + standardShippingCost);
        }
    }

    /** The sum of the lines, before tax and shipping. */
    public Money subtotalOf(List<OrderLine> lines) {
        if (lines == null) {
            return Money.ZERO;
        }
        return lines.stream().map(OrderLine::lineTotal).reduce(Money.ZERO, Money::plus);
    }

    public Money taxOn(Money subtotal) {
        return subtotal.times(taxRate);
    }

    /** Free above the threshold, and only strictly above it — exactly $30 still pays. */
    public Money shippingCostFor(Money subtotal) {
        return subtotal.isGreaterThan(freeShippingThreshold) ? Money.ZERO : standardShippingCost;
    }

    /**
     * What the customer is charged for these lines: subtotal, plus tax on the subtotal, plus
     * shipping. Each step rounds to the cent as it goes, which is what the original
     * {@code itemsSubtotal.plus(taxes).plus(shippingCost)} did.
     */
    public Money totalFor(List<OrderLine> lines) {
        Money subtotal = subtotalOf(lines);
        return subtotal.plus(taxOn(subtotal)).plus(shippingCostFor(subtotal));
    }
}
