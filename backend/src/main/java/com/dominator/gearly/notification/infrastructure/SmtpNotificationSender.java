package com.dominator.gearly.notification.infrastructure;

import com.dominator.gearly.notification.domain.Notification;
import com.dominator.gearly.notification.domain.NotificationDeliveryException;
import com.dominator.gearly.notification.domain.NotificationSender;
import com.dominator.gearly.notification.domain.NotificationType;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Delivers notifications as HTML email over SMTP.
 *
 * <h2>The layout is a resource, loaded once</h2>
 * It was a 120-line Java text block in the middle of {@code EmailService.sendEmail}, which had
 * two costs beyond the obvious one. Every literal {@code %} in the markup had to be written
 * {@code %%} to survive {@code String.formatted} — the table width really was {@code 100%%} in
 * the source — and the five values were positional, so inserting a paragraph meant renumbering
 * an argument list five items long with no compiler help if you got it wrong. Named placeholders
 * in a file that a browser can open fix both.
 *
 * <p>Read at construction rather than per send: it is a packaged resource that cannot change at
 * runtime, and a missing template should fail the context starting rather than the first
 * customer's registration.
 *
 * <h2>The recipient's name is escaped</h2>
 * {@code EmailService} substituted it raw. It is whatever the customer typed at registration, so
 * a name containing markup was rendered as markup in an email — including an {@code <a>} to
 * somewhere else, in a message the recipient has every reason to trust. The wording from
 * {@link NotificationType} is <em>not</em> escaped, because it carries intentional
 * {@code <strong>} tags and is ours.
 */
@Component
public class SmtpNotificationSender implements NotificationSender {

    private static final String LAYOUT = "notification/email-layout.html";

    private final JavaMailSender mailSender;
    private final String layout;

    public SmtpNotificationSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.layout = readLayout();
    }

    @Override
    public void send(Notification notification) {
        NotificationType type = notification.type();
        String html = render(Map.of(
                "name", escape(notification.recipientName()),
                "intro", type.intro(),
                "actionUrl", escape(notification.actionUrl()),
                "actionLabel", type.actionLabel(),
                "disclaimer", type.disclaimer()));

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(notification.recipient().value());
            helper.setSubject(type.subject());
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new NotificationDeliveryException(
                    "Could not send the " + type + " message to " + notification.recipient(), e);
        }
    }

    private String render(Map<String, String> values) {
        String rendered = layout;
        for (Map.Entry<String, String> value : values.entrySet()) {
            rendered = rendered.replace("{{" + value.getKey() + "}}", value.getValue());
        }
        return rendered;
    }

    /**
     * The five characters that matter in both element and attribute context. The action URL is
     * substituted inside {@code href="…"}, so the quote is what stops a crafted link from
     * closing the attribute and adding another.
     */
    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static String readLayout() {
        try (var stream = new ClassPathResource(LAYOUT).getInputStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Missing email layout on the classpath: " + LAYOUT, e);
        }
    }
}
