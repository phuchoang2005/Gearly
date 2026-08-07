package com.dominator.gearly.payments.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Settings for the exchange-rate lookup, bound from {@code gearly.fx.*}.
 *
 * <p>The defaults reproduce the pre-S13 behaviour exactly — the same endpoint and the same
 * 23000 VND figure — so this is configuration of an existing constant, not a change to what
 * customers are charged. What changed is that the constant now has a name, a home and a log
 * line when it is used.
 *
 * @param apiUrl        the rate endpoint; must answer {@code {"rates":{"VND":…}}}
 * @param fallbackUsdToVnd the rate used when the endpoint has never answered successfully
 * @param timeout       how long a checkout is willing to wait for a rate
 * @param maxRateAge    how long a previously fetched rate stays preferable to {@code fallbackUsdToVnd}
 */
@ConfigurationProperties(prefix = "gearly.fx")
public record FxProperties(
        String apiUrl,
        BigDecimal fallbackUsdToVnd,
        Duration timeout,
        Duration maxRateAge) {

    public FxProperties {
        if (apiUrl == null || apiUrl.isBlank()) {
            apiUrl = "https://open.er-api.com/v6/latest/USD";
        }
        if (fallbackUsdToVnd == null) {
            fallbackUsdToVnd = new BigDecimal("23000");
        }
        if (timeout == null) {
            timeout = Duration.ofSeconds(5);
        }
        if (maxRateAge == null) {
            maxRateAge = Duration.ofHours(12);
        }
        if (fallbackUsdToVnd.signum() <= 0) {
            throw new IllegalArgumentException(
                    "gearly.fx.fallback-usd-to-vnd must be positive, got " + fallbackUsdToVnd);
        }
    }
}
