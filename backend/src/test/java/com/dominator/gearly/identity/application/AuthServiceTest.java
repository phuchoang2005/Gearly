package com.dominator.gearly.identity.application;

import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.UnauthorizedException;
import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.EmailAlreadyRegisteredException;
import com.dominator.gearly.identity.domain.PasswordHasher;
import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserFixture;
import com.dominator.gearly.identity.domain.UserNotFoundException;
import com.dominator.gearly.identity.domain.UserRegistered;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.identity.domain.VerificationToken;
import com.dominator.gearly.identity.domain.VerificationTokenTtl;
import com.dominator.gearly.service.common.AddressService;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Credentials, against the identity aggregate.
 *
 * <p>Every assertion the S2/S9 suite made is still here and still means the same thing; what
 * changed is that the fixture builds a real {@link User} through {@code User.register} instead
 * of assigning five fields, and the service takes a {@link UserId} where it took an
 * {@code AuthenticatedUser}. Both follow from the aggregate having no setters, and both are why
 * these tests no longer need a security context to run.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository users;
    @Mock private AccessTokens accessTokens;
    @Mock private AddressService addressService;
    @Mock private VerificationTokenService verificationTokenService;

    private final PasswordHasher passwordHasher = UserFixture.FAKE_HASHER;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(users, accessTokens, passwordHasher,
                addressService, verificationTokenService);
    }

    private User verifiedUser(String email, String rawPassword) {
        return UserFixture.aUser().withId("u1").withEmail(email).withPassword(rawPassword).build();
    }

    private RegisterUserCommand registration(String email) {
        return new RegisterUserCommand("Ada", "Lovelace", email, "pw", "0123456789",
                "1 Main St", "Hanoi", "Hanoi", "100000", "Vietnam");
    }

    // ---- login -------------------------------------------------------------

    @Test
    void login_success_returnsTokenAndUser() {
        User user = verifiedUser("a@b.com", "pw");
        when(users.findByEmail(EmailAddress.of("a@b.com"))).thenReturn(Optional.of(user));
        when(accessTokens.issueFor(EmailAddress.of("a@b.com"))).thenReturn("jwt-token");

        SignedIn res = authService.login("a@b.com", "pw");

        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.user().getEmail()).isEqualTo(EmailAddress.of("a@b.com"));
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("x@y.com", "pw"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        when(users.findByEmail(EmailAddress.of("a@b.com")))
                .thenReturn(Optional.of(verifiedUser("a@b.com", "pw")));

        assertThatThrownBy(() -> authService.login("a@b.com", "bad"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_unverifiedAccount_throwsUnauthorized() {
        User user = UserFixture.aUser().withEmail("a@b.com").unverified().build();
        when(users.findByEmail(EmailAddress.of("a@b.com"))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("a@b.com", "secret"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("verify your email");
    }

    @Test
    @DisplayName("a deactivated account is refused, with the message the storefront matches on")
    void login_inactiveAccount_throwsUnauthorized() {
        User user = UserFixture.aUser().withEmail("a@b.com").withPassword("pw").inactive().build();
        when(users.findByEmail(EmailAddress.of("a@b.com"))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login("a@b.com", "pw"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("a malformed address is a 400, not the 500 an unmapped IllegalArgumentException would be")
    void login_malformedEmail_isBadRequest() {
        assertThatThrownBy(() -> authService.login("not-an-email", "pw"))
                .isInstanceOf(BadRequestException.class);
    }

    // ---- registration ------------------------------------------------------

    @Test
    void register_existingEmail_throwsConflict() {
        when(users.existsByEmail(EmailAddress.of("a@b.com"))).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registration("a@b.com")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("registration saves the account and raises UserRegistered rather than sending mail itself")
    void register_newUser_savesAndRaisesTheEvent() {
        when(users.existsByEmail(EmailAddress.of("new@b.com"))).thenReturn(false);
        when(addressService.getCountryIdByName(any())).thenReturn(1);
        when(addressService.getStateIdByName(any(), anyInt())).thenReturn(2);
        when(addressService.getCityIdByName(any(), anyInt(), anyInt())).thenReturn(3);

        authService.register(registration("new@b.com"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());

        User user = saved.getValue();
        assertThat(user.getEmail()).isEqualTo(EmailAddress.of("new@b.com"));
        assertThat(user.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(user.isVerified()).isFalse();
        // The password reached the field hashed, and the raw value is nowhere on the aggregate.
        assertThat(user.getPasswordHash()).isEqualTo("hashed(pw)");
        assertThat(user.hasPassword("pw", passwordHasher)).isTrue();

        assertThat(user.pullDomainEvents()).singleElement()
                .isInstanceOfSatisfying(UserRegistered.class, event ->
                        assertThat(event.email()).isEqualTo(EmailAddress.of("new@b.com")));

        // The mail is the listener's job now; registration must not send one inline.
        verify(verificationTokenService, never()).createAndSend(any(), any());
    }

    @Test
    @DisplayName("a client-supplied fullName cannot reach the aggregate — there is nowhere for it to land")
    void register_derivesFullNameFromTheParts() {
        when(users.existsByEmail(any())).thenReturn(false);
        when(addressService.getCountryIdByName(any())).thenReturn(1);
        when(addressService.getStateIdByName(any(), anyInt())).thenReturn(2);
        when(addressService.getCityIdByName(any(), anyInt(), anyInt())).thenReturn(3);

        authService.register(new RegisterUserCommand("Grace", "Hopper", "grace@b.com", "pw",
                null, null, null, null, null, null));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().getFullName()).isEqualTo("Grace Hopper");
        assertThat(saved.getValue().getFirstName()).isEqualTo("Grace");
        assertThat(saved.getValue().getLastName()).isEqualTo("Hopper");
    }

    // ---- password reset and change ----------------------------------------

    @Test
    void resetPassword_validToken_updatesHashAndConsumesToken() {
        User user = verifiedUser("a@b.com", "old");
        VerificationToken vt = VerificationToken.issue(
                UserId.of("u1"), VerificationToken.TokenType.PASSWORD_RESET, VerificationTokenTtl.DEFAULT);
        when(verificationTokenService.validate("tok", VerificationToken.TokenType.PASSWORD_RESET))
                .thenReturn(vt);
        when(users.findById(UserId.of("u1"))).thenReturn(Optional.of(user));

        authService.resetPassword("tok", "newpw");

        assertThat(user.hasPassword("newpw", passwordHasher)).isTrue();
        verify(users).save(user);
        verify(verificationTokenService).delete(vt);
    }

    @Test
    void resetPassword_userGone_is404() {
        VerificationToken vt = VerificationToken.issue(
                UserId.of("u1"), VerificationToken.TokenType.PASSWORD_RESET, VerificationTokenTtl.DEFAULT);
        when(verificationTokenService.validate("tok", VerificationToken.TokenType.PASSWORD_RESET))
                .thenReturn(vt);
        when(users.findById(UserId.of("u1"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("tok", "newpw"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void changePassword_wrongOldPassword_throwsBadRequest() {
        User user = verifiedUser("a@b.com", "right");
        when(users.findById(UserId.of("u1"))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.changePassword(UserId.of("u1"), "wrong", "new"))
                .isInstanceOf(BadRequestException.class);
        verify(users, never()).save(any());
    }

    @Test
    @DisplayName("changePassword loads the caller's account rather than trusting a principal handed in")
    void changePassword_loadsTheAggregate() {
        User user = verifiedUser("a@b.com", "right");
        when(users.findById(UserId.of("u1"))).thenReturn(Optional.of(user));

        authService.changePassword(UserId.of("u1"), "right", "brandnew");

        assertThat(user.hasPassword("brandnew", passwordHasher)).isTrue();
        verify(users).save(user);
    }

    @Test
    void handleForgotPassword_unverifiedAccount_throwsBadRequest() {
        User user = UserFixture.aUser().withEmail("a@b.com").unverified().build();
        when(users.findByEmail(EmailAddress.of("a@b.com"))).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.handleForgotPassword("a@b.com"))
                .isInstanceOf(BadRequestException.class);
        verify(verificationTokenService, never()).createAndSend(any(), any());
    }

    @Test
    void handleForgotPassword_unknownEmail_is404() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.handleForgotPassword("nobody@b.com"))
                .isInstanceOf(UserNotFoundException.class);
    }
}
