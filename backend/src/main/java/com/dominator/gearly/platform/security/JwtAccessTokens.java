package com.dominator.gearly.platform.security;

import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.shared.domain.EmailAddress;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * JSON Web Tokens behind {@link AccessTokens}. Was {@code security.JwtUtil}.
 *
 * <p>The signing key, the algorithm and the seven-day lifetime are unchanged, so every token
 * already in a browser keeps working.
 *
 * <h2>What changed, and why it is a security fix</h2>
 * {@code JwtUtil} offered {@code extractEmail} and {@code validateToken} as two independent
 * calls, the first of which parsed and the second of which parsed again inside a
 * {@code try/catch}. {@code JwtAuthenticationFilter} called them in the wrong order — extract,
 * then validate — so an expired or tampered token threw {@code JwtException} out of the filter
 * before anything had a chance to reject it politely. A throw from a servlet filter is not an
 * authentication failure the security chain knows how to render; it became a <b>500</b>, so
 * every customer whose week-old session had just lapsed met a server error instead of a login
 * prompt.
 *
 * <p>There is one method now, and it cannot be called in the wrong order because there is no
 * order. An unreadable token is an empty {@link Optional}, and the only thing a caller can do
 * with that is treat the request as anonymous.
 *
 * <p>A token whose subject is not a well-formed address is also empty rather than an
 * exception: the subject is attacker-influenced input the moment the signing key is
 * compromised, and {@code EmailAddress} throws on a malformed value.
 */
@Component
public class JwtAccessTokens implements AccessTokens {

    /** Unchanged: seven days, as {@code JwtUtil} had it. */
    private static final Duration LIFETIME = Duration.ofDays(7);

    private final Key key;

    public JwtAccessTokens(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issueFor(EmailAddress subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject.value())
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + LIFETIME.toMillis()))
                .signWith(key)
                .compact();
    }

    @Override
    public Optional<EmailAddress> subjectOf(String token) {
        try {
            String subject = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            return subject == null ? Optional.empty() : Optional.of(EmailAddress.of(subject));
        } catch (JwtException | IllegalArgumentException unreadable) {
            return Optional.empty();
        }
    }
}
