package com.dominator.gearly.notification.domain;

/**
 * A message that could not be handed to the delivery mechanism.
 *
 * <p>Not a {@code DomainRuleViolationException}: nothing about the request was wrong, and the
 * caller has done nothing to correct. It is an infrastructure failure with a name, which is
 * what {@code EmailService}'s {@code throw new RuntimeException("Failed to send email", e)} was
 * missing — a bare {@code RuntimeException} cannot be caught selectively, so a listener wanting
 * to tolerate a mail failure had to catch everything.
 */
public class NotificationDeliveryException extends RuntimeException {

    public NotificationDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
