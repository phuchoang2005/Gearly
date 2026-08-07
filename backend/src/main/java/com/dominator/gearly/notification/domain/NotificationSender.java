package com.dominator.gearly.notification.domain;

/**
 * The one way this system talks to a customer out of band.
 *
 * <p>Every context that needs to reach a person names this and nothing else — not
 * {@code JavaMailSender}, not a {@code MimeMessage}, not an SMTP host. Before S13 the only
 * sender was {@code service.user.EmailService}, a concrete class that built MIME messages,
 * carried 120 lines of inline HTML and hard-coded {@code http://localhost:8080} into two links,
 * and {@code identity.application} depended on it directly.
 *
 * <h2>Delivery is not transactional and callers must treat it that way</h2>
 * An SMTP server that accepts a message and then fails has already delivered it, so there is
 * nothing to roll back. Callers publish an event and handle it {@code AFTER_COMMIT} — see
 * {@code VerificationMailListener} for the reasoning — which means a failure here leaves a real
 * account with no mail, not a half-created one.
 */
public interface NotificationSender {

    /**
     * Delivers the message, or throws {@link NotificationDeliveryException} if it could not be
     * handed over.
     */
    void send(Notification notification);
}
