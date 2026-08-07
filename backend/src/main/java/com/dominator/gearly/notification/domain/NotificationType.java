package com.dominator.gearly.notification.domain;

/**
 * The message catalogue — <em>what</em> each notification says.
 *
 * <p>This is the half of a template that is neither presentation nor a business rule: the
 * subject line, the sentence explaining why the mail arrived, the words on the button, and the
 * "if this wasn't you" note. It lives in the domain because it is plain text with no framework
 * in sight, and because it is the part a person would actually want to edit.
 *
 * <p>The other half — the HTML skeleton the words are poured into — is
 * {@code notification/email-layout.html}, loaded by the adapter. Splitting them this way means
 * changing the wording never touches markup and restyling never touches copy.
 *
 * <p>Adding a message type is adding a constant here. Nothing about <em>when</em> it is sent is
 * decided in this context; a message is sent because some other context published an event.
 */
public enum NotificationType {

    EMAIL_VERIFICATION(
            "Confirm Your Gearly Account",
            "Thank you for creating a <strong>Gearly</strong> account. To start shopping PC "
                    + "components, please verify your email address by clicking the button below.",
            "Verify My Email",
            "If you did not create a Gearly account, please ignore this email."),

    PASSWORD_RESET(
            "Reset Your Gearly Password",
            "We received a request to reset your <strong>Gearly</strong> account password. "
                    + "Click the button below to continue.",
            "Reset Password",
            "If you did not request a password reset, you can safely ignore this email.");

    private final String subject;
    private final String intro;
    private final String actionLabel;
    private final String disclaimer;

    NotificationType(String subject, String intro, String actionLabel, String disclaimer) {
        this.subject = subject;
        this.intro = intro;
        this.actionLabel = actionLabel;
        this.disclaimer = disclaimer;
    }

    public String subject() {
        return subject;
    }

    /** The opening sentence. Carries deliberate inline markup, so the adapter must not escape it. */
    public String intro() {
        return intro;
    }

    public String actionLabel() {
        return actionLabel;
    }

    public String disclaimer() {
        return disclaimer;
    }
}
