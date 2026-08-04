/**
 * <b>Payments — generic subdomain / anti-corruption layer.</b> Wraps the MoMo payment
 * gateway and the USD→VND exchange-rate lookup behind ports, so no core context ever
 * names a gateway, a signature scheme or a currency conversion.
 *
 * <p><b>Ports (in {@code payments.domain}):</b> {@code PaymentGateway} —
 * create a payment URL, verify an IPN callback; {@code ExchangeRateProvider} — quote a
 * rate. <b>Adapters (in {@code payments.infrastructure}):</b> {@code MomoPaymentGateway},
 * the HMAC-SHA256 signing helper (one copy, constant-time comparison) and the FX client.
 *
 * <p><b>Relationships:</b> <b>Ordering → Payments</b>, one-way, through
 * {@code PaymentGateway}. Payments knows the {@code OrderId} it was handed and nothing
 * else about the order.
 *
 * <p><b>This package touches money.</b> Signature verification and amount conversion live
 * here; changes are verified against the MoMo sandbox before merging.
 *
 * <p>Filled in by <b>Sprint 13</b>.
 */
package com.dominator.gearly.payments;
