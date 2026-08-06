package com.dominator.gearly.platform.security;

import com.dominator.gearly.identity.domain.PasswordHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt behind {@link PasswordHasher}.
 *
 * <p>In {@code platform.security} rather than in {@code identity.infrastructure} on purpose.
 * The fitness function {@code security_types_stop_at_the_api_layer} forbids <em>any</em> class
 * in a bounded context from naming {@code org.springframework.security..}, and this sprint
 * tightens that rule rather than carving an exception into it. Cryptography is a cross-cutting
 * platform concern, the plan's target architecture already lists {@code platform/security} as
 * one, and platform is allowed to know about the contexts — the dependency runs the right way.
 *
 * <p>The encoder is the same bean {@code SecurityConfig} has always published, so no stored
 * hash changes and every existing password keeps working.
 */
@Component
@RequiredArgsConstructor
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder;

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String storedHash) {
        return encoder.matches(rawPassword, storedHash);
    }
}
