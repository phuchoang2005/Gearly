package com.dominator.gearly.payments.domain;

/**
 * A correctly signed callback whose contents this system cannot read — an unparseable body, or
 * a result code that is not a number.
 *
 * <p>{@code PaymentController} used to run {@code Integer.parseInt(p.resultCode())} unguarded,
 * so a payload with {@code "resultCode": "OK"} answered <b>500</b>. That told the gateway its
 * delivery had failed, which for MoMo means retry — so a payload that could never be parsed was
 * re-delivered on a schedule. A 400 tells it the notification itself is the problem.
 *
 * <p>Because verification runs first, reaching this state means the gateway signed something
 * this code does not understand: a provider-side format change, and worth an alert rather than
 * a silent default.
 */
public class MalformedGatewayNotificationException extends GatewayNotificationRejectedException {

    public MalformedGatewayNotificationException(String message) {
        super(message);
    }
}
