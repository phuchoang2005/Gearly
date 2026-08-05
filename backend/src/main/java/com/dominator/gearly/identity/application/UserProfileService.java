package com.dominator.gearly.identity.application;

import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserNotFoundException;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.service.user.AvatarStorageService;
import com.dominator.gearly.shared.domain.Address;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.PersonName;
import com.dominator.gearly.shared.domain.PhoneNumber;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * The customer's own account: profile edits, avatar, closing it.
 *
 * <p>Was {@code service.user.UserService}. Every method took an {@code AuthenticatedUser} and
 * read {@code authenticatedUser.getUser()} — so the use case operated on whatever object the
 * security filter happened to have loaded, and could not be exercised at all without a security
 * context. They take a {@link UserId} and load the aggregate, which is also what makes the
 * write correct: the principal is built once per request by the filter, so a long request that
 * saved it twice was writing a stale copy the second time.
 *
 * <p>File storage is still {@code AvatarStorageService}. S13 replaces it with a
 * {@code FileStorage} port and adds the content-type and size validation it does not have.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository users;
    private final AccessTokens accessTokens;
    private final AvatarStorageService avatarStorage;

    /**
     * Save the profile and hand back a fresh token.
     *
     * <p>The token is reissued because the email address is the subject of the JWT: change it
     * without a new token and the next request authenticates as an address that no longer
     * exists. That has always been the behaviour; it is stated here because it is not obvious.
     */
    public SignedIn updateProfile(UserId caller, UpdateProfileCommand command) {
        User user = require(caller);
        user.updateProfile(
                PersonName.of(command.firstName(), command.lastName()),
                emailOf(command.email()),
                phoneOf(command.phone()),
                command.address());
        users.save(user);

        return new SignedIn(accessTokens.issueFor(user.getEmail()), user);
    }

    public void uploadAvatar(UserId caller, MultipartFile file) throws IOException {
        User user = require(caller);
        user.changeAvatar(avatarStorage.store(user.getId(), file));
        users.save(user);
    }

    public void deactivate(UserId caller) {
        User user = require(caller);
        user.deactivate();
        users.save(user);
    }

    private User require(UserId caller) {
        return users.findById(caller).orElseThrow(() -> new UserNotFoundException(caller));
    }

    private EmailAddress emailOf(String value) {
        try {
            return EmailAddress.of(value);
        } catch (IllegalArgumentException malformed) {
            throw new BadRequestException(malformed.getMessage());
        }
    }

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

    /** What the profile screen submits. {@link Address} is shared-kernel, so it passes through. */
    public record UpdateProfileCommand(String firstName,
                                       String lastName,
                                       String email,
                                       String phone,
                                       Address address) {
    }
}
