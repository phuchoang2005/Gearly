package com.dominator.gearly.identity.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The identity context's tunables, bound from {@code gearly.identity.*}.
 *
 * <p>Currently one: how long a verification or password-reset token stays usable. It was a
 * {@code private static final Duration} on {@code VerificationTokenService} — a policy compiled
 * into a service, which meant shortening it for a security incident required a rebuild. The
 * default is the thirty minutes it always was.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gearly.identity")
public class IdentityProperties {

    private Duration verificationTokenTtl = Duration.ofMinutes(30);
}
