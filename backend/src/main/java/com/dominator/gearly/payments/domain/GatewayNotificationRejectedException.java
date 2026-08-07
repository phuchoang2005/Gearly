package com.dominator.gearly.payments.domain;

import com.dominator.gearly.shared.domain.DomainRuleViolationException;

/**
 * A callback this system refuses to act on: either it was not signed by the gateway, or it was
 * signed but cannot be understood.
 *
 * <p>The two cases are separate subclasses because they mean different things operationally —
 * one is someone forging a payment notification, the other is a protocol change on the
 * provider's side — but they get the same answer on the wire, because telling an unauthenticated
 * caller which of the two happened tells them something about the key.
 */
public abstract class GatewayNotificationRejectedException extends DomainRuleViolationException {

    protected GatewayNotificationRejectedException(String message) {
        super(message);
    }
}
