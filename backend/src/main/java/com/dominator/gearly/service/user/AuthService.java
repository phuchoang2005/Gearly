package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.LoginResponseDTO;
import com.dominator.gearly.dto.ResetPasswordRequestDTO;
import com.dominator.gearly.dto.UserLoginRequestDTO;
import com.dominator.gearly.dto.UserRegisterRequestDTO;
import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ConflictException;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.exception.UnauthorizedException;
import com.dominator.gearly.mapper.UserMapper;
import com.dominator.gearly.shared.domain.Address;
import com.dominator.gearly.model.User;
import com.dominator.gearly.model.UserStatus;
import com.dominator.gearly.model.VerificationToken;
import com.dominator.gearly.repository.UserRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import com.dominator.gearly.security.JwtUtil;
import com.dominator.gearly.service.common.AddressService;
import com.dominator.gearly.shared.domain.PersonName;
import com.dominator.gearly.shared.domain.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication and credential flows: login/JWT issuance, registration, and
 * password change/reset. Verification-token issuing and consumption is delegated
 * to {@link VerificationTokenService}.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AddressService addressService;
    private final VerificationTokenService verificationTokenService;
    private final UserMapper userMapper;

    public LoginResponseDTO login(UserLoginRequestDTO req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedException("This account had been set to inactive. \nPlease contact Gearly Support if you need to activate your account.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponseDTO(token, userMapper.toResponseDto(user));
    }

    @Transactional
    public void register(UserRegisterRequestDTO req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ConflictException("Email already registered.");
        }

        int countryId = addressService.getCountryIdByName(req.getCountry());
        int stateId = addressService.getStateIdByName(req.getState(), countryId);
        int cityId = addressService.getCityIdByName(req.getCity(), stateId, countryId);
        Address address = new Address(
                req.getStreetAddress(),
                req.getCity(),
                cityId,
                req.getState(),
                stateId,
                req.getPostalCode(),
                req.getCountry(),
                countryId
        );

        User user = new User();
        user.setEmail(req.getEmail());
        // Derived from the parts, not taken from req.getFullName(). The request still
        // carries a fullName for backward compatibility, but honouring it is what let a
        // registration store a display name that disagreed with its own first and last
        // name — with nothing to ever reconcile the two. See User.setName.
        user.setName(PersonName.of(req.getFirstName(), req.getLastName()));
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(Role.CUSTOMER);
        user.setVerified(false);
        user.setAddress(address);

        userRepository.save(user);

        verificationTokenService.createAndSend(user, VerificationToken.TokenType.EMAIL_VERIFICATION);
    }

    public void handleForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not registered."));

        if (!user.isVerified()) {
            throw new BadRequestException("Please verify your email before resetting password.");
        }

        verificationTokenService.createAndSend(user, VerificationToken.TokenType.PASSWORD_RESET);
    }

    public void resetPassword(ResetPasswordRequestDTO req) {
        VerificationToken vt = verificationTokenService.validate(
                req.getToken(), VerificationToken.TokenType.PASSWORD_RESET);

        User user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        verificationTokenService.delete(vt);
    }

    public void changePassword(AuthenticatedUser authUser, String oldPassword, String newPassword) {
        User user = authUser.getUser();
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadRequestException("Old password does not match with your current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
