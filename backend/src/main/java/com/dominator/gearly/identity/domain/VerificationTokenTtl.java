package com.dominator.gearly.identity.domain;

import java.time.Duration;

/**
 * How long a verification or password-reset token stays usable.
 *
 * <p>A named type rather than a bare {@link Duration} for the same reason
 * {@code LowStockThreshold} is one in the catalog: it is a policy the business owns, it is
 * bound from configuration ({@code gearly.identity.verification-token-ttl}), and a method
 * taking a {@code Duration} says nothing about which duration it wants.
 */
public record VerificationTokenTtl(Duration duration) {

    /** What {@code VerificationTokenService} had hard-coded. */
    public static final VerificationTokenTtl DEFAULT = new VerificationTokenTtl(Duration.ofMinutes(30));

    public VerificationTokenTtl {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            throw new IllegalArgumentException(
                    "a verification token must be usable for a positive duration, was " + duration);
        }
    }
}
