package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.LoginResponseDTO;
import com.dominator.bookify.dto.UserUpdateRequestDTO;
import com.dominator.bookify.mapper.UserMapper;
import com.dominator.bookify.model.User;
import com.dominator.bookify.model.UserStatus;
import com.dominator.bookify.repository.UserRepository;
import com.dominator.bookify.security.AuthenticatedUser;
import com.dominator.bookify.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * User profile and account operations. Authentication/credentials live in
 * {@link AuthService}, verification tokens in {@link VerificationTokenService},
 * and avatar file storage in {@link AvatarStorageService}.
 */
@RequiredArgsConstructor
@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AvatarStorageService avatarStorageService;
    private final UserMapper userMapper;

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
        return new LoginResponseDTO(token, userMapper.toResponseDto(user));
    }

    public void uploadAvatar(AuthenticatedUser authenticatedUser, MultipartFile file) throws IOException {
        User user = authenticatedUser.getUser();
        String publicPath = avatarStorageService.store(user.getId(), file);
        user.setProfileAvatar(publicPath);
        userRepository.save(user);
    }

    public void deactiveUser(AuthenticatedUser authenticatedUser) {
        User user = authenticatedUser.getUser();
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }
}
