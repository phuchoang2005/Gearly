package com.dominator.gearly.identity.domain;

/**
 * How a password is turned into something safe to store, and how a candidate is checked
 * against it. A port: the identity context states that passwords are hashed and never
 * compared directly, and says nothing about BCrypt.
 *
 * <h2>Why this is a port rather than an injected {@code BCryptPasswordEncoder}</h2>
 * Two reasons, and only one of them is purity. The first is that
 * {@code org.springframework.security.crypto} is a Spring Security package, and the fitness
 * function {@code security_types_stop_at_the_api_layer} bans a context from naming one — a
 * rule S12 tightens rather than relaxes, because "the domain must be constructible in a plain
 * unit test" stops being true the moment a security type appears in a constructor.
 *
 * <p>The second is that {@link User} needs this at the point where the password is set. A
 * service that hashed the password and handed the aggregate the result would leave
 * {@code passwordHash} settable from outside with anything at all — which is how three call
 * sites came to each do their own {@code encode}, and how a fourth that forgot would have
 * compiled. Passing the hasher in means the raw value cannot reach the field.
 *
 * <p>The adapter is {@code platform.security.BCryptPasswordHasher}, which is where the
 * cryptography choice belongs: one place, changeable without the domain noticing.
 */
public interface PasswordHasher {

    /** @return the stored form of {@code rawPassword} */
    String hash(String rawPassword);

    /** @return whether {@code rawPassword} produced {@code storedHash} */
    boolean matches(String rawPassword, String storedHash);
}
