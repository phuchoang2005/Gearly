package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.LoginResponseDTO;
import com.dominator.gearly.dto.UserUpdateRequestDTO;
import com.dominator.gearly.mapper.UserMapper;
import com.dominator.gearly.model.User;
import com.dominator.gearly.model.UserStatus;
import com.dominator.gearly.repository.UserRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import com.dominator.gearly.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private AvatarStorageService avatarStorageService;
    @Spy private UserMapper userMapper = new UserMapper();

    @InjectMocks private UserService userService;

    private AuthenticatedUser authUserOf(User user) {
        return new AuthenticatedUser(user);
    }

    private User existingUser() {
        User u = new User();
        u.setId("u1");
        u.setEmail("old@b.com");
        u.setStatus(UserStatus.ACTIVE);
        return u;
    }

    @Test
    void updateProfile_updatesFields_savesAndReturnsFreshToken() {
        User user = existingUser();
        when(jwtUtil.generateToken("new@b.com")).thenReturn("jwt-token");

        UserUpdateRequestDTO req = new UserUpdateRequestDTO();
        req.setFirstName("Jane");
        req.setLastName("Doe");
        req.setEmail("new@b.com");
        req.setPhone("0123");

        LoginResponseDTO res = userService.updateProfile(authUserOf(user), req);

        assertThat(user.getFullName()).isEqualTo("Jane Doe");
        assertThat(user.getEmail()).isEqualTo("new@b.com");
        assertThat(user.getPhone()).isEqualTo("0123");
        verify(userRepository).save(user);
        assertThat(res.getToken()).isEqualTo("jwt-token");
        assertThat(res.getUser().getEmail()).isEqualTo("new@b.com");
    }

    @Test
    void uploadAvatar_storesFile_setsPublicPathAndSaves() throws IOException {
        User user = existingUser();
        MultipartFile file = mock(MultipartFile.class);
        when(avatarStorageService.store("u1", file)).thenReturn("/uploads/avatars/u1.jpg");

        userService.uploadAvatar(authUserOf(user), file);

        assertThat(user.getProfileAvatar()).isEqualTo("/uploads/avatars/u1.jpg");
        verify(userRepository).save(user);
    }

    @Test
    void deactiveUser_setsStatusInactiveAndSaves() {
        User user = existingUser();

        userService.deactiveUser(authUserOf(user));

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(userRepository).save(user);
    }
}
