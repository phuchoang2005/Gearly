package com.dominator.gearly.identity.infrastructure;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * The identity context's tunables, bound from {@code gearly.identity.*}.
 *
 * <p>How long a verification or password-reset token stays usable was a
 * {@code private static final Duration} on {@code VerificationTokenService} — a policy compiled
 * into a service, which meant shortening it for a security incident required a rebuild. The
 * default is the thirty minutes it always was.
 *
 * <p>{@code publicBaseUrl} is where this backend is reachable from a customer's mail client.
 * It was {@code "http://localhost:8080"} written into two links in the same service, so every
 * verification mail a deployed environment sent pointed at the recipient's own machine.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gearly.identity")
public class IdentityProperties {

    private Duration verificationTokenTtl = Duration.ofMinutes(30);

    private String publicBaseUrl = "http://localhost:8080";
}
