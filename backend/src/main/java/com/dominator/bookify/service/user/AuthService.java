package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.LoginResponseDTO;
import com.dominator.bookify.dto.ResetPasswordRequestDTO;
import com.dominator.bookify.dto.UserLoginRequestDTO;
import com.dominator.bookify.dto.UserRegisterRequestDTO;
import com.dominator.bookify.dto.UserResponseDTO;
import com.dominator.bookify.exception.BadRequestException;
import com.dominator.bookify.exception.ConflictException;
import com.dominator.bookify.exception.ResourceNotFoundException;
import com.dominator.bookify.exception.UnauthorizedException;
import com.dominator.bookify.model.Address;
import com.dominator.bookify.model.User;
import com.dominator.bookify.model.UserStatus;
import com.dominator.bookify.model.VerificationToken;
import com.dominator.bookify.repository.UserRepository;
import com.dominator.bookify.security.AuthenticatedUser;
import com.dominator.bookify.security.JwtUtil;
import com.dominator.bookify.service.common.AddressService;
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

    public LoginResponseDTO login(UserLoginRequestDTO req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!user.isVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in.");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UnauthorizedException("This account had been set to inactive. \nPlease contact Bookify Support if you need to activate your account.");
        }

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new LoginResponseDTO(token, convertToUserDTO(user));
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
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole("CUSTOMER");
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

    // TODO(S4): replace with UserMapper (also duplicated in UserService).
    private UserResponseDTO convertToUserDTO(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setProfileAvatar(user.getProfileAvatar());
        dto.setFullName(user.getFullName());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFavorites(user.getFavorites());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setVerified(user.isVerified());
        dto.setStatus(user.getStatus());
        dto.setAddress(user.getAddress());
        return dto;
    }
}
