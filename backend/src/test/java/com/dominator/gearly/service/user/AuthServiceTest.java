package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.LoginResponseDTO;
import com.dominator.gearly.dto.ResetPasswordRequestDTO;
import com.dominator.gearly.dto.UserLoginRequestDTO;
import com.dominator.gearly.dto.UserRegisterRequestDTO;
import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ConflictException;
import com.dominator.gearly.exception.UnauthorizedException;
import com.dominator.gearly.mapper.UserMapper;
import com.dominator.gearly.model.User;
import com.dominator.gearly.model.UserStatus;
import com.dominator.gearly.model.VerificationToken;
import com.dominator.gearly.repository.UserRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import com.dominator.gearly.security.JwtUtil;
import com.dominator.gearly.service.common.AddressService;
import com.dominator.gearly.shared.domain.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private BCryptPasswordEncoder passwordEncoder;
    @Mock private AddressService addressService;
    @Mock private VerificationTokenService verificationTokenService;
    @Spy private UserMapper userMapper = new UserMapper();

    @InjectMocks private AuthService authService;

    private User verifiedUser(String email, String hash) {
        User u = new User();
        u.setId("u1");
        u.setEmail(email);
        u.setPasswordHash(hash);
        u.setVerified(true);
        u.setStatus(UserStatus.ACTIVE);
        u.setRole(Role.CUSTOMER);
        return u;
    }

    @Test
    void login_success_returnsTokenAndUser() {
        User user = verifiedUser("a@b.com", "hash");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", "hash")).thenReturn(true);
        when(jwtUtil.generateToken("a@b.com")).thenReturn("jwt-token");

        UserLoginRequestDTO req = new UserLoginRequestDTO();
        req.setEmail("a@b.com");
        req.setPassword("pw");

        LoginResponseDTO res = authService.login(req);

        assertThat(res.getToken()).isEqualTo("jwt-token");
        assertThat(res.getUser().getEmail()).isEqualTo("a@b.com");
    }

    @Test
    void login_unknownEmail_throwsUnauthorized() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        UserLoginRequestDTO req = new UserLoginRequestDTO();
        req.setEmail("x@y.com");
        req.setPassword("pw");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_wrongPassword_throwsUnauthorized() {
        User user = verifiedUser("a@b.com", "hash");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        UserLoginRequestDTO req = new UserLoginRequestDTO();
        req.setEmail("a@b.com");
        req.setPassword("bad");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void login_unverifiedAccount_throwsUnauthorized() {
        User user = verifiedUser("a@b.com", "hash");
        user.setVerified(false);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        UserLoginRequestDTO req = new UserLoginRequestDTO();
        req.setEmail("a@b.com");
        req.setPassword("pw");

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void register_existingEmail_throwsConflict() {
        UserRegisterRequestDTO req = new UserRegisterRequestDTO();
        req.setEmail("a@b.com");
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(ConflictException.class);
        verify(verificationTokenService, never()).createAndSend(any(), any());
    }

    @Test
    void register_newUser_savesAndSendsVerification() {
        UserRegisterRequestDTO req = new UserRegisterRequestDTO();
        req.setEmail("new@b.com");
        req.setPassword("pw");
        // Both are @NotBlank on the request, so a real call can never reach the service
        // without them; the stored full name is now derived from these two.
        req.setFirstName("Ada");
        req.setLastName("Lovelace");
        when(userRepository.findByEmail("new@b.com")).thenReturn(Optional.empty());
        when(addressService.getCountryIdByName(any())).thenReturn(1);
        when(addressService.getStateIdByName(any(), anyInt())).thenReturn(2);
        when(addressService.getCityIdByName(any(), anyInt(), anyInt())).thenReturn(3);
        when(passwordEncoder.encode("pw")).thenReturn("hashed");

        authService.register(req);

        verify(userRepository).save(any(User.class));
        verify(verificationTokenService)
                .createAndSend(any(User.class), eq(VerificationToken.TokenType.EMAIL_VERIFICATION));
    }

    @Test
    void resetPassword_validToken_updatesHashAndConsumesToken() {
        VerificationToken vt = new VerificationToken();
        vt.setUserId("u1");
        when(verificationTokenService.validate("tok", VerificationToken.TokenType.PASSWORD_RESET))
                .thenReturn(vt);
        User user = verifiedUser("a@b.com", "old");
        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newpw")).thenReturn("newhash");

        ResetPasswordRequestDTO req = new ResetPasswordRequestDTO();
        req.setToken("tok");
        req.setNewPassword("newpw");

        authService.resetPassword(req);

        assertThat(user.getPasswordHash()).isEqualTo("newhash");
        verify(userRepository).save(user);
        verify(verificationTokenService).delete(vt);
    }

    @Test
    void changePassword_wrongOldPassword_throwsBadRequest() {
        User user = verifiedUser("a@b.com", "hash");
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);
        AuthenticatedUser authUser = new AuthenticatedUser(user);

        assertThatThrownBy(() -> authService.changePassword(authUser, "wrong", "new"))
                .isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any());
    }
}
