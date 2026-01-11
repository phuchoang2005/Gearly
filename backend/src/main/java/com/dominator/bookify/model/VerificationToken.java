package com.dominator.bookify.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Getter
@Setter
@Document("verification_tokens")
public class VerificationToken {
    @Id
    private String id;
    private String userId;
    private String token;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private TokenType type;

    public enum TokenType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }
}

