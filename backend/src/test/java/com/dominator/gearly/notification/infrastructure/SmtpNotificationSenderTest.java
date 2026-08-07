package com.dominator.gearly.notification.infrastructure;

import com.dominator.gearly.notification.domain.Notification;
import com.dominator.gearly.notification.domain.NotificationDeliveryException;
import com.dominator.gearly.notification.domain.NotificationType;
import com.dominator.gearly.shared.domain.EmailAddress;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The SMTP adapter: what actually gets composed and handed to the mail server.
 *
 * <p>The message is read back off the {@link MimeMessage} rather than off an intermediate
 * string, so what is asserted is what would be sent.
 */
@ExtendWith(MockitoExtension.class)
class SmtpNotificationSenderTest {

    @Mock private JavaMailSender mailSender;

    private SmtpNotificationSender sender;

    @BeforeEach
    void setUp() {
        sender = new SmtpNotificationSender(mailSender);
    }

    private MimeMessage capture(Notification notification) throws Exception {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        sender.send(notification);

        ArgumentCaptor<MimeMessage> sent = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(sent.capture());

        // Content-Type headers are derived from the content when the message is saved, which a
        // real transport does on the way out. Without it every part still reports the default
        // text/plain and nothing can be located by MIME type.
        sent.getValue().saveChanges();
        return sent.getValue();
    }

    /**
     * The HTML body, dug out of the MIME tree.
     *
     * <p>{@code MimeMessageHelper(message, true, …)} builds a multipart message — that is what
     * the {@code true} means, and it is inherited from {@code EmailService} unchanged — so the
     * message's own content is a {@code MimeMultipart} and the markup is a part inside it.
     */
    private static String htmlBodyOf(Part part) throws Exception {
        if (part.isMimeType("text/html")) {
            return (String) part.getContent();
        }
        if (part.getContent() instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                String found = htmlBodyOf(multipart.getBodyPart(i));
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private String htmlFor(Notification notification) throws Exception {
        String html = htmlBodyOf(capture(notification));
        assertThat(html).as("the message carries an HTML part").isNotNull();
        return html;
    }

    private static Notification verificationTo(String name) {
        return new Notification(
                EmailAddress.of("ada@example.com"),
                NotificationType.EMAIL_VERIFICATION,
                name,
                "http://gearly.test/api/users/verify?token=abc&tokenType=EMAIL_VERIFICATION");
    }

    @Test
    @DisplayName("addresses the recipient, uses the type's subject, and sends HTML")
    void composesTheMessage() throws Exception {
        MimeMessage message = capture(verificationTo("Ada"));

        assertThat(message.getAllRecipients()[0].toString()).isEqualTo("ada@example.com");
        assertThat(message.getSubject()).isEqualTo("Confirm Your Gearly Account");
        assertThat(htmlBodyOf(message)).isNotNull();
    }

    @Test
    @DisplayName("renders the type's wording and the caller's link into the layout")
    void rendersTheLayout() throws Exception {
        String html = htmlFor(verificationTo("Ada"));

        assertThat(html)
                .contains("Hello, Ada")
                .contains("Thank you for creating a <strong>Gearly</strong> account")
                .contains("Verify My Email")
                .contains("If you did not create a Gearly account")
                .contains("href=\"http://gearly.test/api/users/verify?token=abc"
                        + "&amp;tokenType=EMAIL_VERIFICATION\"");
    }

    @Test
    @DisplayName("no placeholder survives into a sent message")
    void leavesNoPlaceholders() throws Exception {
        String html = htmlFor(verificationTo("Ada"));

        assertThat(html).doesNotContain("{{").doesNotContain("}}");
    }

    /**
     * The width attribute in the layout is a literal {@code 100%}. In the Java text block it had
     * to be written {@code 100%%} because the whole thing went through {@code String.formatted};
     * in a resource file it is just a percent sign, and this asserts the output did not change
     * when it moved.
     */
    @Test
    @DisplayName("the layout renders a literal 100% width, as the text block did")
    void percentSurvivesTheMoveOutOfATextBlock() throws Exception {
        String html = htmlFor(verificationTo("Ada"));

        assertThat(html).contains("width=\"100%\"").doesNotContain("100%%");
    }

    /**
     * <b>A fix, not a refactor.</b> {@code EmailService} substituted the name with {@code %s}
     * straight into the HTML. The name is whatever the customer typed at registration, so a
     * registration under {@code <a href=…>} put an attacker's link into a message from Gearly
     * that the recipient has every reason to trust.
     */
    @Test
    @DisplayName("a name containing markup is escaped, not rendered")
    void escapesTheRecipientName() throws Exception {
        String html = htmlFor(verificationTo("<a href=\"http://evil.test\">click</a>"));

        assertThat(html).doesNotContain("<a href=\"http://evil.test\">");
        assertThat(html).contains("&lt;a href=&quot;http://evil.test&quot;&gt;");
    }

    @Test
    @DisplayName("the type's own markup is left alone")
    void doesNotEscapeOurOwnWording() throws Exception {
        String html = htmlFor(verificationTo("Ada"));

        assertThat(html).contains("<strong>Gearly</strong>");
    }

    @Test
    @DisplayName("a password reset uses its own subject, wording and button")
    void passwordResetIsADifferentMessage() throws Exception {
        MimeMessage message = capture(new Notification(
                EmailAddress.of("ada@example.com"),
                NotificationType.PASSWORD_RESET,
                "Ada",
                "http://gearly.test/reset"));

        assertThat(message.getSubject()).isEqualTo("Reset Your Gearly Password");
        assertThat(htmlBodyOf(message))
                .contains("Reset Password")
                .contains("If you did not request a password reset");
    }

    @Test
    @DisplayName("a blank name still addresses the reader")
    void blankNameFallsBack() throws Exception {
        assertThat(htmlFor(verificationTo("  "))).contains("Hello, there");
    }

    /**
     * {@code EmailService} threw a bare {@code RuntimeException}, which a caller wanting to
     * tolerate a mail failure could only catch by catching everything.
     */
    @Test
    @DisplayName("a refused send becomes a NotificationDeliveryException")
    void wrapsDeliveryFailures() {
        when(mailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("connection refused")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> sender.send(verificationTo("Ada")))
                .isInstanceOf(NotificationDeliveryException.class)
                .hasMessageContaining("ada@example.com")
                .hasRootCauseMessage("connection refused");
    }
}
