package com.dominator.gearly.identity.infrastructure;

import com.dominator.gearly.identity.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Spring Data's view of the users collection. Package-private in spirit: nothing outside this
 * adapter package may name it — {@code spring_data_repositories_live_only_in_infrastructure}
 * enforces that — and everything else goes through
 * {@link com.dominator.gearly.identity.domain.UserRepository}.
 *
 * <p>The derived queries the admin console used are gone; {@code MongoUserRepository.search}
 * builds them as criteria instead. {@code findByEmail} stays derived, but takes the raw string:
 * the field is an {@code EmailAddress} on the aggregate and a plain string on disk, and a
 * derived query compares against what is stored.
 */
interface SpringDataUserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
