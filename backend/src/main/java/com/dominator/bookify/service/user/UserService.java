package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.*;
import com.dominator.bookify.exception.BadRequestException;
import com.dominator.bookify.exception.ConflictException;
import com.dominator.bookify.exception.ResourceNotFoundException;
import com.dominator.bookify.exception.UnauthorizedException;
import com.dominator.bookify.model.Address;
import com.dominator.bookify.model.User;
import com.dominator.bookify.model.UserStatus;
import com.dominator.bookify.model.VerificationToken;
import com.dominator.bookify.repository.UserRepository;
import com.dominator.bookify.repository.VerificationTokenRepository;
import com.dominator.bookify.security.AuthenticatedUser;
import com.dominator.bookify.security.JwtUtil;
import com.dominator.bookify.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class UserService {
    private final String avatarDir = Paths.get(System.getProperty("user.dir"))
            .resolve("frontend")
            .resolve("public")
            .resolve("data")
            .resolve("User Avatars")
            .toString();

    private final UserRepository userRepository;

    private final JwtUtil jwtUtil;

    private final BCryptPasswordEncoder passwordEncoder;

    private final EmailService emailService;

    private final VerificationTokenRepository verificationTokenRepo;

    private final AddressService addressService;

    public LoginResponseDTO updateProfile(AuthenticatedUser authenticatedUser, UserUpdateRequestDTO req) {
        User user = authenticatedUser.getUser();
        user.setFirstName(req.getFirstName());
        user.setLastName(req.getLastName());
        user.setFullName(req.getFirstName() + " " + req.getLastName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setAddress(req.getAddress());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getEmail());

        return new LoginResponseDTO(token, convertToUserDTO(user));
    }

    public void uploadAvatar(AuthenticatedUser authenticatedUser, MultipartFile file) throws IOException {
        User user = authenticatedUser.getUser();
        String userId = user.getId();
        String extension = ".jpg";
        String filename = userId + extension;
        File directory = new File(avatarDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        File dest = new File(directory, filename);
        file.transferTo(dest);
        String publicPath = "/data/User Avatars/" + filename;
        user.setProfileAvatar(publicPath);
        userRepository.save(user);
    }

    public void deactiveUser(AuthenticatedUser authenticatedUser) {
        User user = authenticatedUser.getUser();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

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

    public void register(UserRegisterRequestDTO req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ConflictException("Email already registered.");
        }

        int countryId = addressService.getCountryIdByName(req.getCountry());
        int stateId = addressService.getStateIdByName(req.getState(), countryId);
        int cityId = addressService.getCityIdByName(req.getCity(),stateId,countryId);
        // Map address
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

        createAndSendToken(user, "EMAIL_VERIFICATION");
    }

    public void verifyToken(String token, VerificationToken.TokenType tokenType) {
        VerificationToken vt = verificationTokenRepo
                .findByTokenAndType(token, tokenType)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token"));

        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token expired!");
        }

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
        createAndSendToken(user, "EMAIL_VERIFICATION");
    }

    private void createAndSendToken(User user, String tokenType) {
        String token = UUID.randomUUID().toString();
        VerificationToken vt = new VerificationToken();
        vt.setUserId(user.getId());
        vt.setToken(token);
        vt.setCreatedAt(LocalDateTime.now());
        vt.setExpiresAt(LocalDateTime.now().plusMinutes(30));
        vt.setType(tokenType.equals("EMAIL_VERIFICATION") ? VerificationToken.TokenType.EMAIL_VERIFICATION : VerificationToken.TokenType.PASSWORD_RESET);
        verificationTokenRepo.save(vt);

        if (tokenType.equals("EMAIL_VERIFICATION")) {
            emailService.sendVerificationEmail(user.getEmail(), user.getFullName(), token);
        } else {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), token);
        }
    }

    public void handleForgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Email not registered."));

        if (!user.isVerified()) {
            throw new BadRequestException("Please verify your email before resetting password.");
        }

        createAndSendToken(user, "PASSWORD_RESET");
    }

    public void resetPassword(ResetPasswordRequestDTO req) {
        VerificationToken vt = verificationTokenRepo
                .findByTokenAndType(req.getToken(), VerificationToken.TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new BadRequestException("Invalid or expired token."));

        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Token has expired.");
        }

        User user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(user);

        verificationTokenRepo.delete(vt);
    }

    public void changePassword(AuthenticatedUser authUser, String oldPassword, String newPassword) {
        User user = authUser.getUser();
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BadRequestException("Old password does not match with your current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

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
