package com.dominator.gearly.identity.infrastructure;

import com.dominator.gearly.identity.domain.VerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** Spring Data's view of the verification_tokens collection. */
interface SpringDataVerificationTokenRepository extends MongoRepository<VerificationToken, String> {

    Optional<VerificationToken> findByTokenAndType(String token, VerificationToken.TokenType type);

    void deleteByUserId(String userId);
}
