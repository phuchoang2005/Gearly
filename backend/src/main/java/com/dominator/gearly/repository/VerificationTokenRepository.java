package com.dominator.gearly.repository;

import com.dominator.gearly.model.VerificationToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends MongoRepository<VerificationToken, String> {
    Optional<VerificationToken> findByTokenAndType(String token, VerificationToken.TokenType type);
    Optional<VerificationToken> findByToken(String token);
    void deleteByUserId(String userId);
}
