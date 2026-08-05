package com.dominator.gearly.identity.application;

import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserFixture;
import com.dominator.gearly.identity.domain.UserNotFoundException;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.identity.domain.UserStatus;
import com.dominator.gearly.service.user.AvatarStorageService;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.PhoneNumber;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The customer's own account operations.
 *
 * <p>Was {@code UserServiceTest}. The behaviour asserted is identical; the difference is that
 * each call now names a {@link UserId} and the service loads the aggregate, where before it
 * operated on whatever {@code User} instance the security principal was carrying. The extra
 * {@code findById} stub in every test is that difference made visible — and it is the point:
 * the principal is built once per request by the filter, so a use case that wrote through it
 * was writing through a snapshot.
 */
@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserRepository users;
    @Mock private AccessTokens accessTokens;
    @Mock private AvatarStorageService avatarStorage;

    private UserProfileService userProfileService;

    private static final UserId CALLER = UserId.of("u1");

    @BeforeEach
    void setUp() {
        userProfileService = new UserProfileService(users, accessTokens, avatarStorage);
    }

    private User existingUser() {
        return UserFixture.aUser().withId("u1").withEmail("old@b.com").build();
    }

    @Test
    void updateProfile_updatesFields_savesAndReturnsFreshToken() {
        User user = existingUser();
        when(users.findById(CALLER)).thenReturn(Optional.of(user));
        when(accessTokens.issueFor(EmailAddress.of("new@b.com"))).thenReturn("jwt-token");

        SignedIn res = userProfileService.updateProfile(CALLER,
                new UserProfileService.UpdateProfileCommand("Jane", "Doe", "new@b.com", "0123456789", null));

        assertThat(user.getFullName()).isEqualTo("Jane Doe");
        assertThat(user.getEmail()).isEqualTo(EmailAddress.of("new@b.com"));
        assertThat(user.getPhone()).isEqualTo(PhoneNumber.of("0123456789"));
        verify(users).save(user);
        assertThat(res.token()).isEqualTo("jwt-token");
        assertThat(res.user().getEmail()).isEqualTo(EmailAddress.of("new@b.com"));
    }

    @Test
    @DisplayName("the token is reissued for the new address, because the address is the token's subject")
    void updateProfile_reissuesForTheNewEmail() {
        User user = existingUser();
        when(users.findById(CALLER)).thenReturn(Optional.of(user));
        when(accessTokens.issueFor(EmailAddress.of("moved@b.com"))).thenReturn("fresh");

        SignedIn res = userProfileService.updateProfile(CALLER,
                new UserProfileService.UpdateProfileCommand("Jane", "Doe", "moved@b.com", "0123456789", null));

        assertThat(res.token()).isEqualTo("fresh");
        verify(accessTokens).issueFor(EmailAddress.of("moved@b.com"));
    }

    @Test
    void uploadAvatar_storesFile_setsPublicPathAndSaves() throws IOException {
        User user = existingUser();
        MultipartFile file = mock(MultipartFile.class);
        when(users.findById(CALLER)).thenReturn(Optional.of(user));
        when(avatarStorage.store("u1", file)).thenReturn("/uploads/avatars/u1.jpg");

        userProfileService.uploadAvatar(CALLER, file);

        assertThat(user.getProfileAvatar()).isEqualTo("/uploads/avatars/u1.jpg");
        verify(users).save(user);
    }

    @Test
    void deactivate_setsStatusInactiveAndSaves() {
        User user = existingUser();
        when(users.findById(CALLER)).thenReturn(Optional.of(user));

        userProfileService.deactivate(CALLER);

        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(user.isActive()).isFalse();
        verify(users).save(user);
    }

    @Test
    @DisplayName("a caller whose account has been deleted is a 404, not a null dereference")
    void missingAccount_is404() {
        when(users.findById(CALLER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.deactivate(CALLER))
                .isInstanceOf(UserNotFoundException.class);
    }
}
