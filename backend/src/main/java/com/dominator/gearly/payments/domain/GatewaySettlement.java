package com.dominator.gearly.payments.domain;

/**
 * What a payment gateway told us about an order, with the gateway's dialect already removed.
 *
 * <p>This is the whole of Payments' outbound published language. Ordering consumes it and
 * knows nothing else: not that the provider is MoMo, not that {@code resultCode == 0} is what
 * success looks like there, not that the gateway prefixes our order id with {@code Gearly-}.
 * Each of those is a MoMo fact and each used to be spread across the controller, the service
 * and the aggregate.
 *
 * @param orderReference  <em>our</em> order id — the gateway's own prefixing already undone
 * @param transactionId   the gateway's identifier for the movement of money, stored on the ledger
 * @param successful      whether the money actually moved
 * @param rawNotification the notification exactly as received, kept verbatim for disputes
 */
public record GatewaySettlement(
        String orderReference,
        String transactionId,
        boolean successful,
        String rawNotification) {
}
