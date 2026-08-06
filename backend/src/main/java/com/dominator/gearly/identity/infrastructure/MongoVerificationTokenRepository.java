package com.dominator.gearly.identity.infrastructure;

import com.dominator.gearly.identity.domain.VerificationToken;
import com.dominator.gearly.identity.domain.VerificationTokenRepository;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** The MongoDB adapter behind {@link VerificationTokenRepository}. */
@Repository
@RequiredArgsConstructor
public class MongoVerificationTokenRepository implements VerificationTokenRepository {

    private final SpringDataVerificationTokenRepository tokens;

    @Override
    public Optional<VerificationToken> findByTokenAndType(String token, VerificationToken.TokenType type) {
        return tokens.findByTokenAndType(token, type);
    }

    @Override
    public VerificationToken save(VerificationToken token) {
        return tokens.save(token);
    }

    @Override
    public void delete(VerificationToken token) {
        tokens.delete(token);
    }

    @Override
    public void deleteAllFor(UserId userId) {
        tokens.deleteByUserId(userId.value());
    }
}
