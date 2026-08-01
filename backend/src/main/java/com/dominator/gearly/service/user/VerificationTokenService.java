package com.dominator.gearly.service.user;

import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ConflictException;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.model.User;
import com.dominator.gearly.model.VerificationToken;
import com.dominator.gearly.repository.UserRepository;
import com.dominator.gearly.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Owns the verification-token lifecycle (email verification + password reset):
 * issuing tokens, triggering the matching email, and validating/consuming them.
 */
@Service
@RequiredArgsConstructor
public class VerificationTokenService {

    private final VerificationTokenRepository verificationTokenRepo;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /** Issue a fresh token for the user and send the matching email. */
    public void createAndSend(User user, VerificationToken.TokenType type) {
        VerificationToken vt = new VerificationToken();
        vt.setUserId(user.getId());
        vt.setToken(UUID.randomUUID().toString());
        vt.setCreatedAt(LocalDateTime.now());
        vt.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        vt.setType(type);
        verificationTokenRepo.save(vt);

        if (type == VerificationToken.TokenType.EMAIL_VERIFICATION) {
            emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), vt.getToken());
        } else {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), vt.getToken());
        }
    }

    /** Return the token if it exists and has not expired, otherwise throw. */
    public VerificationToken validate(String token, VerificationToken.TokenType type) {
        VerificationToken vt = verificationTokenRepo.findByTokenAndType(token, type)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token expired!");
        }
        return vt;
    }

    public void delete(VerificationToken vt) {
        verificationTokenRepo.delete(vt);
    }

    /** Consume an email-verification token and flip the user's verified flag. */
    public void verifyToken(String token, VerificationToken.TokenType tokenType) {
        VerificationToken vt = validate(token, tokenType);

        User user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        if (tokenType == VerificationToken.TokenType.EMAIL_VERIFICATION) {
            if (user.isVerified()) {
                throw new ConflictException("Account already verified.");
            }
            user.setVerified(true);
            userRepository.save(user);
            verificationTokenRepo.delete(vt);
        }
    }

    public void resendVerification(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not registered"));

        if (user.isVerified()) {
            verificationTokenRepo.deleteByUserId(user.getId());
            throw new ConflictException("User already verified.");
        }

        verificationTokenRepo.deleteByUserId(user.getId());
        createAndSend(user, VerificationToken.TokenType.EMAIL_VERIFICATION);
    }
}
