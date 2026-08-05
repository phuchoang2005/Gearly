package com.dominator.gearly.identity.infrastructure;

import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.EmailAddress;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The MongoDB adapter behind {@link UserRepository}. The only class in the identity context
 * that knows a user is stored in MongoDB, that the email is a plain string on disk, or that
 * "name contains" is a case-insensitive regex.
 */
@Repository
@RequiredArgsConstructor
public class MongoUserRepository implements UserRepository {

    private final SpringDataUserRepository users;
    private final MongoTemplate mongoTemplate;
    private final ApplicationEventPublisher events;

    @Override
    public Optional<User> findById(UserId id) {
        return users.findById(id.value());
    }

    @Override
    public Optional<User> findByEmail(EmailAddress email) {
        return users.findByEmail(email.value());
    }

    @Override
    public List<User> findAllById(List<UserId> ids) {
        return users.findAllById(ids.stream().map(UserId::value).toList());
    }

    @Override
    public List<User> findAll() {
        return users.findAll();
    }

    @Override
    public boolean existsByEmail(EmailAddress email) {
        return users.existsByEmail(email.value());
    }

    /**
     * Writes the aggregate, then publishes whatever it recorded while it was being changed —
     * the same contract {@code MongoOrderRepository.save} has, for the same reasons: one place
     * that publishes, so a use case cannot forget, and nothing announced for a change that
     * failed to persist.
     */
    @Override
    public User save(User user) {
        User saved = users.save(user);
        for (DomainEvent event : saved.pullDomainEvents()) {
            events.publishEvent(event);
        }
        return saved;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The term is {@link Pattern#quote}d, as the order search's is. Spring Data's derived
     * {@code ContainingIgnoreCase} did this for free; building the criteria by hand means doing
     * it deliberately, and not doing it would let an admin's search box put a regular
     * expression into the query — at best a confusing result set, at worst a scan that does not
     * terminate.
     */
    @Override
    public List<User> search(String fullNameLike, String emailLike) {
        Query query = new Query();
        if (isPresent(fullNameLike)) {
            query.addCriteria(Criteria.where("fullName").regex(contains(fullNameLike), "i"));
        }
        if (isPresent(emailLike)) {
            query.addCriteria(Criteria.where("email").regex(contains(emailLike), "i"));
        }
        return mongoTemplate.find(query, User.class);
    }

    private static boolean isPresent(String filter) {
        return filter != null && !filter.isBlank();
    }

    private static String contains(String term) {
        return ".*" + Pattern.quote(term) + ".*";
    }
}
