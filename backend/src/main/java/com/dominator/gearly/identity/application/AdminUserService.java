package com.dominator.gearly.identity.application;

import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserNotFoundException;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The admin console's view of accounts: listing, searching, and switching one on or off.
 *
 * <p>The four-branch {@code if/else} that picked between three derived repository methods is
 * gone — {@link UserRepository#search} takes both filters and applies whichever are present, so
 * the decision is made once, in the adapter, in terms of criteria rather than of method names.
 */
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository users;

    public List<User> getAllUsers(String fullNameLike, String emailLike) {
        return users.search(fullNameLike, emailLike);
    }

    public User getUserById(String id) {
        return require(UserId.of(id));
    }

    /** @return {@code true}, preserving the {@code ResponseEntity<Boolean>} the console reads */
    public boolean activateUser(String id) {
        User user = require(UserId.of(id));
        user.activate();
        users.save(user);
        return true;
    }

    public boolean deactivateUser(String id) {
        User user = require(UserId.of(id));
        user.deactivate();
        users.save(user);
        return true;
    }

    private User require(UserId id) {
        return users.findById(id).orElseThrow(() -> new UserNotFoundException("User not found"));
    }
}
