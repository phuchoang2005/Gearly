package com.dominator.gearly.identity.application;

import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ConflictException;
import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserNotFoundException;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.identity.domain.VerificationToken;
import com.dominator.gearly.identity.domain.VerificationTokenRepository;
import com.dominator.gearly.identity.domain.VerificationTokenTtl;
import com.dominator.gearly.service.user.EmailService;
import com.dominator.gearly.shared.domain.EmailAddress;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Issuing, sending, validating and consuming verification and password-reset tokens.
 *
 * <p>The thirty-minute lifetime is no longer a constant in this class: it arrives as
 * {@link VerificationTokenTtl}, bound from {@code gearly.identity.verification-token-ttl}, and
 * the token computes its own expiry from it. The default is what was compiled in before.
 *
 * <p>{@code EmailService} is still the legacy sender. S13 puts a {@code NotificationSender} port
 * in front of it; the only thing that has to change here when it does is the field.
 */
@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository tokens;
    private final UserRepository users;
    private final EmailService emailService;
    private final VerificationTokenTtl ttl;

    /** Issue a fresh token for the user and send the matching email. */
    public void createAndSend(User user, VerificationToken.TokenType type) {
        VerificationToken vt = tokens.save(VerificationToken.issue(user.userId(), type, ttl));

        if (type == VerificationToken.TokenType.EMAIL_VERIFICATION) {
            emailService.sendVerificationEmail(user.getEmail().value(), user.displayName(), vt.getToken());
        } else {
            emailService.sendPasswordResetEmail(user.getEmail().value(), user.displayName(), vt.getToken());
        }
    }

    /** Return the token if it exists and has not expired, otherwise throw. */
    public VerificationToken validate(String token, VerificationToken.TokenType type) {
        VerificationToken vt = tokens.findByTokenAndType(token, type)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        if (vt.isExpired(Instant.now())) {
            throw new BadRequestException("Token expired!");
        }
        return vt;
    }

    public void delete(VerificationToken vt) {
        tokens.delete(vt);
    }

    /**
     * Consume an email-verification token and flip the account's verified flag.
     *
     * <p>"Already verified" is {@code User.verify()}'s refusal now rather than an
     * {@code if (user.isVerified())} here — same 409, same message, but stated by the thing
     * that owns the flag, so no other path can set it without the check.
     */
    public void verifyToken(String token, VerificationToken.TokenType tokenType) {
        VerificationToken vt = validate(token, tokenType);

        if (!vt.isOfType(VerificationToken.TokenType.EMAIL_VERIFICATION)) {
            return;
        }

        User user = users.findById(vt.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found!"));

        user.verify();
        users.save(user);
        tokens.delete(vt);
    }

    /**
     * Re-send a verification mail, replacing any outstanding token.
     *
     * <p>The outstanding tokens are cleared in both branches, including the one that then
     * refuses — a verified account has no use for a pending verification token, and leaving one
     * behind is what let a stale link keep working after the account was already confirmed.
     */
    public void resendVerification(String email) {
        User user = users.findByEmail(emailOf(email))
                .orElseThrow(() -> new UserNotFoundException("Email not registered"));

        tokens.deleteAllFor(user.userId());

        if (user.isVerified()) {
            throw new ConflictException("User already verified.");
        }

        createAndSend(user, VerificationToken.TokenType.EMAIL_VERIFICATION);
    }

    private EmailAddress emailOf(String value) {
        try {
            return EmailAddress.of(value);
        } catch (IllegalArgumentException malformed) {
            throw new BadRequestException(malformed.getMessage());
        }
    }
}
