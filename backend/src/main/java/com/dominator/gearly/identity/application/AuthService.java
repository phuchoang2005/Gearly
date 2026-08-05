package com.dominator.gearly.identity.application;

import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.UnauthorizedException;
import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.EmailAlreadyRegisteredException;
import com.dominator.gearly.identity.domain.PasswordHasher;
import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserNotFoundException;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.identity.domain.VerificationToken;
import com.dominator.gearly.service.common.AddressService;
import com.dominator.gearly.shared.domain.Address;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.PersonName;
import com.dominator.gearly.shared.domain.PhoneNumber;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Credentials: signing in, signing up, and changing or resetting a password.
 *
 * <p>Profile edits live in {@link UserProfileService}, tokens in
 * {@link VerificationTokenService}, and the moderation console in {@link AdminUserService}.
 *
 * <h2>What moved out of here</h2>
 * <ul>
 *   <li><b>The security types.</b> {@code changePassword} took an {@code AuthenticatedUser} —
 *       a Spring Security {@code UserDetails} — so it could not be called without a security
 *       context, and "who is asking" had become an input to the use case rather than a
 *       decision the edge had already made. It takes a {@link UserId}.</li>
 *   <li><b>Hashing.</b> Every method that touched a password did its own
 *       {@code encoder.encode(...)}. The aggregate takes a {@link PasswordHasher} and sets the
 *       field itself, so a path that forgot to hash no longer compiles.</li>
 *   <li><b>The mail send.</b> {@code register} was {@code @Transactional} and called
 *       {@code createAndSend} as its last statement, holding a database transaction open across
 *       an SMTP conversation. It publishes {@code UserRegistered} now — see
 *       {@link VerificationMailListener}.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final AccessTokens accessTokens;
    private final PasswordHasher passwordHasher;
    private final AddressService addressService;
    private final VerificationTokenService verificationTokenService;

    /**
     * Sign in.
     *
     * <p>The three refusals — unverified, deactivated, wrong password — are unchanged, in the
     * same order and with the same messages, because the storefront matches on them.
     */
    public SignedIn login(String email, String rawPassword) {
        User user = users.findByEmail(emailOf(email))
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in.");
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("This account had been set to inactive. \nPlease contact Gearly Support if you need to activate your account.");
        }

        if (!user.hasPassword(rawPassword, passwordHasher)) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return new SignedIn(accessTokens.issueFor(user.getEmail()), user);
    }

    /**
     * Sign up.
     *
     * <p>Still transactional, and now legitimately so: what it covers is one aggregate write.
     * The verification token and the mail that carries it are raised as an event and handled
     * after this commits.
     */
    @Transactional
    public void register(RegisterUserCommand command) {
        EmailAddress email = emailOf(command.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        users.save(User.register(
                PersonName.of(command.firstName(), command.lastName()),
                email,
                command.password(),
                passwordHasher,
                phoneOf(command.phone()),
                resolveAddress(command)));
    }

    public void handleForgotPassword(String email) {
        User user = users.findByEmail(emailOf(email))
                .orElseThrow(() -> new UserNotFoundException("Email not registered."));

        if (!user.isVerified()) {
            throw new BadRequestException("Please verify your email before resetting password.");
        }

        verificationTokenService.createAndSend(user, VerificationToken.TokenType.PASSWORD_RESET);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        VerificationToken vt = verificationTokenService.validate(
                token, VerificationToken.TokenType.PASSWORD_RESET);

        User user = users.findById(vt.getUserId())
                .orElseThrow(() -> new UserNotFoundException("User not found."));

        user.changePassword(newPassword, passwordHasher);
        users.save(user);

        verificationTokenService.delete(vt);
    }

    public void changePassword(UserId caller, String oldPassword, String newPassword) {
        User user = users.findById(caller).orElseThrow(() -> new UserNotFoundException(caller));

        if (!user.hasPassword(oldPassword, passwordHasher)) {
            throw new BadRequestException("Old password does not match with your current password.");
        }

        user.changePassword(newPassword, passwordHasher);
        users.save(user);
    }

    /**
     * The registration form sends place <em>names</em>; the stored address carries the numeric
     * ids the geo lookups use. Unchanged, and still {@code service.common}'s job until S13 puts
     * a port in front of it.
     */
    private Address resolveAddress(RegisterUserCommand command) {
        int countryId = addressService.getCountryIdByName(command.country());
        int stateId = addressService.getStateIdByName(command.state(), countryId);
        int cityId = addressService.getCityIdByName(command.city(), stateId, countryId);
        return new Address(
                command.streetAddress(),
                command.city(),
                cityId,
                command.state(),
                stateId,
                command.postalCode(),
                command.country(),
                countryId);
    }

    /**
     * A malformed address is a 400, not the 500 an unmapped {@code IllegalArgumentException}
     * from the value object would be. Bean validation on the request bodies catches this first
     * for every path that has a body; this is the backstop for the ones that do not.
     */
    private EmailAddress emailOf(String value) {
        try {
            return EmailAddress.of(value);
        } catch (IllegalArgumentException malformed) {
            throw new BadRequestException(malformed.getMessage());
        }
    }

    /** Optional, and refused rather than silently dropped when it is present and malformed. */
    private PhoneNumber phoneOf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PhoneNumber.of(value);
        } catch (IllegalArgumentException malformed) {
            throw new BadRequestException(malformed.getMessage());
        }
    }
}
