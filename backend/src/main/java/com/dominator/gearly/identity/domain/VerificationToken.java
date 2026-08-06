package com.dominator.gearly.identity.domain;

import com.dominator.gearly.shared.domain.UserId;
import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * A single-use secret sent to an email address to prove the recipient controls it — either to
 * verify a new account or to authorise a password reset.
 *
 * <p>Its own aggregate, and deliberately not a field on {@link User}: it has an independent
 * lifecycle (issued, consumed, replaced, expired), several may exist for one account at once,
 * and it is the one thing here that is written without the user being loaded.
 *
 * <h2>The TTL is configuration now</h2>
 * It was {@code Duration.ofMinutes(30)}, a {@code private static final} on
 * {@code VerificationTokenService} — a policy decision compiled into a service. It arrives as a
 * parameter of {@link #issue} now, bound from {@code gearly.identity.verification-token-ttl}.
 * The default is still thirty minutes, so nothing changes unless someone chooses to change it.
 *
 * <h2>Expiry is asked of the token, not of the clock</h2>
 * {@code validate} compared {@code vt.getExpiresAt()} against {@code Instant.now()} inside the
 * service, which is why the rule could not be tested without waiting. {@link #isExpired} takes
 * the instant to compare against.
 */
@Getter
@Document("verification_tokens")
public class VerificationToken {

    @Id
    private String id;

    private UserId userId;

    private String token;

    private Instant createdAt;

    private Instant expiresAt;

    private TokenType type;

    /** For Spring Data. */
    protected VerificationToken() {
    }

    /** Mint a token for {@code userId}, good for {@code ttl} from now. */
    public static VerificationToken issue(UserId userId, TokenType type, VerificationTokenTtl ttl) {
        VerificationToken vt = new VerificationToken();
        vt.userId = Objects.requireNonNull(userId, "a token belongs to a user");
        vt.type = Objects.requireNonNull(type, "a token has a type");
        vt.token = UUID.randomUUID().toString();
        Instant now = Instant.now();
        vt.createdAt = now;
        vt.expiresAt = now.plus(ttl.duration());
        return vt;
    }

    public boolean isExpired(Instant at) {
        return expiresAt != null && expiresAt.isBefore(at);
    }

    public boolean isOfType(TokenType other) {
        return type == other;
    }

    /** What a token is for. The wire form of the {@code tokenType} query parameter — unchanged. */
    public enum TokenType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }
}
