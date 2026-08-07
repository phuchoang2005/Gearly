package com.dominator.gearly.identity.application;

import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserNotFoundException;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.shared.domain.Address;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.PersonName;
import com.dominator.gearly.shared.domain.PhoneNumber;
import com.dominator.gearly.shared.domain.UserId;
import com.dominator.gearly.storage.domain.FileStorage;
import com.dominator.gearly.storage.domain.StorageArea;
import com.dominator.gearly.storage.domain.UploadedFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
 * <p>S13: avatars go through the {@link FileStorage} port, which is what validates them. The
 * signature takes an {@link UploadedFile} rather than Spring's {@code MultipartFile} for the
 * same reason every other method here takes a {@link UserId} — a use case that can only be
 * called with a live HTTP request is a use case that cannot be tested or reused.
 */
@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository users;
    private final AccessTokens accessTokens;
    private final FileStorage fileStorage;

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

    /**
     * Replaces the caller's avatar.
     *
     * <p>Stored under the account's own id, so a customer has exactly one — and the extension
     * follows the uploaded type rather than always being {@code .jpg}, which is what
     * {@code AvatarStorageService} did regardless of what the file actually was.
     */
    public void uploadAvatar(UserId caller, UploadedFile file) {
        User user = require(caller);
        user.changeAvatar(fileStorage.storeAs(StorageArea.AVATARS, user.getId(), file));
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
