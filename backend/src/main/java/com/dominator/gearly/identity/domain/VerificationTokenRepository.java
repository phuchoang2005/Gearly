package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.UserId;

import java.util.Optional;

/** The verification-token port. Adapter: {@code MongoVerificationTokenRepository}. */
public interface VerificationTokenRepository {

    Optional<VerificationToken> findByTokenAndType(String token, VerificationToken.TokenType type);

    VerificationToken save(VerificationToken token);

    void delete(VerificationToken token);

    void deleteAllFor(UserId userId);
}
