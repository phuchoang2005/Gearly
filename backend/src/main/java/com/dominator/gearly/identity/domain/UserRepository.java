package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Identity's repository port: typed ids and value objects in, aggregates out. The MongoDB
 * adapter behind it is {@code MongoUserRepository}.
 *
 * <p>{@link #search} replaces the three derived queries the admin console used to pick between
 * in a service — {@code findAllByFullNameContainingIgnoreCase},
 * {@code findAllByEmailContainingIgnoreCase} and the two-argument combination. Which of them to
 * call was decided by a chain of {@code isBlank()} checks in {@code AdminUserService}, so
 * adding a third filter would have meant eight branches and eight repository methods. One
 * method with two optional criteria says the same thing once.
 */
public interface UserRepository {

    Optional<User> findById(UserId id);

    Optional<User> findByEmail(EmailAddress email);

    List<User> findAllById(List<UserId> ids);

    List<User> findAll();

    boolean existsByEmail(EmailAddress email);

    /** Saves, then publishes whatever the aggregate recorded — see the adapter. */
    User save(User user);

    /**
     * The admin console's user list. Either filter may be {@code null} or blank, meaning
     * "no constraint"; both applied together means both must match, as it did before.
     */
    List<User> search(String fullNameLike, String emailLike);
}
