package com.dominator.gearly.notification.domain;

import com.dominator.gearly.shared.domain.EmailAddress;

import java.util.Objects;

/**
 * One message to one person: who it goes to, which message it is, and where the button leads.
 *
 * <h2>Why the caller supplies the URL and not the token</h2>
 * The obvious alternative — {@code sendVerification(user, token)} — would put Identity's route
 * ({@code /api/users/verify?token=…&tokenType=…}) inside the notification context, so a change
 * to an Identity URL would be a change to a generic subdomain. The context that owns the route
 * builds the link; this context decides what the mail looks like and delivers it. That split is
 * what {@code notification/package-info.java} means by owning the templates and none of the
 * business rules.
 *
 * @param recipient     where it goes
 * @param type          which message — subject, wording and button label come from it
 * @param recipientName how to address them; <b>rendered as escaped text</b>, being user-supplied
 * @param actionUrl     where the button leads
 */
public record Notification(
        EmailAddress recipient,
        NotificationType type,
        String recipientName,
        String actionUrl) {

    public Notification {
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(actionUrl, "actionUrl must not be null");
        if (recipientName == null || recipientName.isBlank()) {
            recipientName = "there";
        }
    }
}
