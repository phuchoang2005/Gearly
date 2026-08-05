package com.dominator.gearly.identity.infrastructure;

import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserDirectory;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Answers {@link UserDirectory} out of the users collection.
 *
 * <p>Thin on purpose: it is a projection of one field, and the point of the port is that the
 * caller never sees the rest. An account with no display name at all is treated as absent — a
 * blank name is not information a review row can show, and every caller already has a phrase
 * for "we do not know who this was".
 */
@Component
@RequiredArgsConstructor
public class MongoUserDirectory implements UserDirectory {

    private final UserRepository users;

    @Override
    public Optional<String> displayNameOf(UserId userId) {
        return users.findById(userId).map(User::displayName).filter(name -> !name.isBlank());
    }

    @Override
    public Map<UserId, String> displayNamesOf(Collection<UserId> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<UserId, String> names = new LinkedHashMap<>();
        for (User user : users.findAllById(List.copyOf(userIds))) {
            String name = user.displayName();
            if (name != null && !name.isBlank()) {
                names.put(user.userId(), name);
            }
        }
        return names;
    }

    @Override
    public List<ProductId> favoritesOf(UserId userId) {
        return users.findById(userId).map(User::getFavorites).orElseGet(List::of);
    }
}
