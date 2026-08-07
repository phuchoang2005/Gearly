package com.dominator.gearly.payments.domain;

/**
 * A gateway callback whose signature does not verify.
 *
 * <p>Anyone on the internet can POST to an IPN endpoint; the signature is the only thing that
 * distinguishes the gateway from someone claiming an order was paid for. So this is thrown
 * before the payload is looked at for any other purpose, and the caller is told nothing about
 * why — a verifier that reports <em>where</em> a signature diverges is an oracle.
 */
public class InvalidGatewaySignatureException extends GatewayNotificationRejectedException {

    public InvalidGatewaySignatureException() {
        super("The payment notification's signature could not be verified");
    }
}
