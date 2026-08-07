package com.dominator.gearly.payments.domain;

import java.math.BigDecimal;

/**
 * Quotes the USD→VND rate the payment gateway is settled in.
 *
 * <p>Every price in this system is USD ({@code Money} carries no other currency), but MoMo
 * settles in VND, so exactly one conversion happens and it happens at the gateway edge. This
 * port is that edge. Nothing in Ordering, Catalog or Cart names it — an order's total is USD
 * before this is called and USD after.
 *
 * <h2>A rate is always returned</h2>
 * There is deliberately no checked exception and no {@code Optional}. A checkout must not fail
 * because a third-party rate service is slow, and the adapter is the layer that knows what to
 * fall back to. What the adapter must <em>not</em> do is fall back silently: see
 * {@link com.dominator.gearly.payments.infrastructure.OpenErApiExchangeRateProvider} for the
 * last-known-good ladder and the logging that replaced a bare {@code catch (Exception ignored)}.
 */
public interface ExchangeRateProvider {

    /** How many VND one USD buys. Never {@code null}, never zero or negative. */
    BigDecimal usdToVnd();
}
