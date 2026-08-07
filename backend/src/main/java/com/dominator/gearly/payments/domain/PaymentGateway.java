package com.dominator.gearly.payments.domain;

import com.dominator.gearly.shared.domain.Money;

/**
 * The external payment provider, as the rest of the system is allowed to see it.
 *
 * <p>Both halves of a hosted-checkout flow are here — sending the customer out, and hearing
 * back — because they are the same protocol and splitting them would put half a signature
 * scheme in each of two places. That is precisely the state S13 found: the request was signed
 * in {@code MomoService} and the callback verified in {@code PaymentController}, with the
 * HMAC-SHA256 helper copy-pasted verbatim between them.
 *
 * <h2>The notification methods take and return raw text on purpose</h2>
 * A gateway's callback is <em>its</em> document, not ours. Handing the adapter the untouched
 * body means every MoMo field name, the order they are concatenated in to be signed, and the
 * shape of the acknowledgement all stay inside the adapter — the controller becomes a pipe and
 * never has to change when a provider revises its payload. It also means
 * {@link GatewaySettlement#rawNotification()} is what actually arrived, byte for byte, which is
 * the only version of it worth keeping when a customer disputes a charge.
 */
public interface PaymentGateway {

    /**
     * Registers a payment and returns the URL the customer is redirected to in order to make it.
     *
     * @param amount         the order total, in the system currency; the adapter converts
     * @param orderReference our order id, which the gateway will echo back on the callback
     */
    String startCheckout(Money amount, String orderReference);

    /**
     * Authenticates a callback and translates it.
     *
     * @throws InvalidGatewaySignatureException if the payload was not signed by the gateway
     */
    GatewaySettlement verifyNotification(String rawNotification);

    /**
     * The signed reply the gateway expects, as the JSON text to write back. Gateways treat a
     * missing or unsigned acknowledgement as a delivery failure and retry, so this is part of
     * the protocol rather than a courtesy.
     */
    String acknowledgementFor(String rawNotification);
}
