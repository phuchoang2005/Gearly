package com.dominator.gearly.service.user;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String fullName, String token) {
        String link = "http://localhost:8080/api/users/verify?token=" + token + "&tokenType=EMAIL_VERIFICATION";

        sendEmail(
                to,
                "Confirm Your Gearly Account",
                fullName,
                link,
                "Verify My Email",
                "Thank you for creating a <strong>Gearly</strong> account. To start shopping PC components, please verify your email address by clicking the button below.",
                "If you did not create a Gearly account, please ignore this email."
        );
    }

    public void sendPasswordResetEmail(String to, String fullName, String token) {
        String link = "http://localhost:8080/api/users/verify?token=" + token + "&tokenType=PASSWORD_RESET";

        sendEmail(
                to,
                "Reset Your Gearly Password",
                fullName,
                link,
                "Reset Password",
                "We received a request to reset your <strong>Gearly</strong> account password. Click the button below to continue.",
                "If you did not request a password reset, you can safely ignore this email."
        );
    }

    private void sendEmail(
            String to,
            String subject,
            String fullName,
            String link,
            String buttonText,
            String intro,
            String disclaimer
    ) {

        String html = """
            <!DOCTYPE html>
            <html>
              <body style="margin:0; padding:0; background-color:#0f0f0f; font-family:Arial, Helvetica, sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0">
                  <tr>
                    <td align="center" style="padding:40px 0;">
                      <table width="600" cellpadding="40" cellspacing="0"
                             style="background-color:#ffffff; border-radius:10px; box-shadow:0 8px 30px rgba(0,0,0,0.35);">

                        <tr>
                          <td align="center" style="padding-bottom:20px;">
                            <h1 style="margin:0; color:#d70018; letter-spacing:1px;">
                              GEARLY
                            </h1>
                            <p style="margin:5px 0 0; color:#666; font-size:14px;">
                              PC Parts & Performance Gear
                            </p>
                          </td>
                        </tr>

                        <tr>
                          <td>
                            <h2 style="color:#111;">Hello, %s</h2>

                            <p style="font-size:16px; color:#333; line-height:1.6;">
                              %s
                            </p>

                            <div style="text-align:center; margin:40px 0;">
                              <a href="%s"
                                 style="
                                   display:inline-block;
                                   background-color:#d70018;
                                   color:#ffffff;
                                   padding:14px 36px;
                                   font-size:16px;
                                   font-weight:600;
                                   text-decoration:none;
                                   border-radius:30px;
                                   box-shadow:0 6px 18px rgba(215,0,24,0.45);
                                 ">
                                %s
                              </a>
                            </div>

                            <p style="font-size:14px; color:#555; line-height:1.6;">
                              %s
                            </p>

                            <hr style="border:none; border-top:1px solid #e5e5e5; margin:40px 0;">

                            <p style="font-size:12px; color:#888; text-align:center;">
                              © 2025 Gearly. All rights reserved.<br>
                              High-Performance PC Store
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td>
                  </tr>
                </table>
              </body>
            </html>
        """.formatted(fullName, intro, link, buttonText, disclaimer);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
