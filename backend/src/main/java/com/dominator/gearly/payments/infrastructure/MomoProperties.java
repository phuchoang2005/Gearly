package com.dominator.gearly.payments.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * MoMo credentials and endpoints, bound from {@code momo.*}.
 *
 * <p>Replaces {@code config.MomoConfig}, which was a mutable {@code @Data} bean in a package
 * every layer could reach. The credentials now live beside the one adapter that uses them, and
 * the record is immutable, so a secret cannot be reassigned at runtime by anything holding a
 * reference to it.
 *
 * <p>The property names are unchanged, so no deployment has to be reconfigured: Spring's
 * relaxed binding maps the existing {@code momo.partnerCode} onto {@code partnerCode} exactly
 * as before.
 *
 * @param createUrl the gateway's create-payment endpoint — configurable rather than the
 *                  hard-coded sandbox URL it was, which is what going live requires changing
 */
@ConfigurationProperties(prefix = "momo")
public record MomoProperties(
        String partnerCode,
        String accessKey,
        String secretKey,
        String returnUrl,
        String notifyUrl,
        String createUrl,
        Duration timeout) {

    public MomoProperties {
        if (createUrl == null || createUrl.isBlank()) {
            createUrl = "https://test-payment.momo.vn/v2/gateway/api/create";
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(10);
        }
    }
}
