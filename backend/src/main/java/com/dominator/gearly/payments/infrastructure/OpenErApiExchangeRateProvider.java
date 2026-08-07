package com.dominator.gearly.payments.infrastructure;

import com.dominator.gearly.payments.domain.ExchangeRateProvider;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * The {@link ExchangeRateProvider} adapter, talking to open.er-api.com.
 *
 * <h2>What this replaces, and why it was worth replacing</h2>
 * The old {@code FxService} was eight lines wrapped in {@code catch (Exception ignored)} with a
 * hard-coded {@code 23000} as the fallback. Three things followed from that, all bad:
 *
 * <ol>
 *   <li><b>Nothing was ever logged.</b> A permanently broken rate lookup and a working one are
 *       indistinguishable from outside the process. The endpoint could have been dead for
 *       months.</li>
 *   <li><b>The fallback was stale by construction.</b> 23000 VND/USD was roughly the rate when
 *       the line was written; it has since drifted past 26000. Falling back charged a customer
 *       ~12% less than the order was worth, silently, on every request that failed.</li>
 *   <li><b>There was no timeout.</b> {@code new RestTemplate()} has none, so a hung third party
 *       hung the checkout — and since S10 moved the gateway call outside the transaction, that
 *       is a held HTTP worker rather than a held database transaction, but it is still a
 *       checkout that never answers.</li>
 * </ol>
 *
 * <p>The ladder is now explicit: <b>a fresh quote</b>, else <b>the last one this process
 * successfully fetched</b> while it is younger than {@code gearly.fx.max-rate-age}, else
 * <b>the configured fallback</b>. Each rung down logs, and the bottom rung logs at
 * {@code ERROR} — a sale priced off a constant is an operational event, not a detail.
 *
 * <p>The cache is deliberately per-process and in-memory. A shared cache is a second piece of
 * infrastructure to run for a value that is re-fetched on the next successful request anyway;
 * the point of holding it is to survive a blip, not to be a rate service.
 */
public class OpenErApiExchangeRateProvider implements ExchangeRateProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenErApiExchangeRateProvider.class);

    private final RestClient http;
    private final FxProperties properties;
    private final Clock clock;

    /** The most recent rate this process actually fetched, or {@code null} if none ever succeeded. */
    private volatile Quote lastGood;

    public OpenErApiExchangeRateProvider(RestClient http, FxProperties properties) {
        this(http, properties, Clock.systemUTC());
    }

    /**
     * The seam the tests use. Taking a built {@link RestClient} rather than a builder is what
     * lets a test bind {@code MockRestServiceServer} to it — a constructor that clones a builder
     * and installs its own request factory would overwrite the mock's.
     */
    OpenErApiExchangeRateProvider(RestClient http, FxProperties properties, Clock clock) {
        this.http = http;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public BigDecimal usdToVnd() {
        try {
            BigDecimal fetched = fetch();
            lastGood = new Quote(fetched, clock.instant());
            return fetched;
        } catch (Exception failure) {
            return degradeFrom(failure);
        }
    }

    private BigDecimal fetch() {
        JsonNode root = http.get().uri(properties.apiUrl()).retrieve().body(JsonNode.class);
        if (root == null) {
            throw new IllegalStateException("rate endpoint returned an empty body");
        }
        JsonNode vnd = root.path("rates").path("VND");
        if (vnd.isMissingNode() || vnd.asText().isEmpty()) {
            throw new IllegalStateException("rate endpoint returned no rates.VND field");
        }
        BigDecimal rate = new BigDecimal(vnd.asText());
        if (rate.signum() <= 0) {
            throw new IllegalStateException("rate endpoint quoted a non-positive rate: " + rate);
        }
        return rate;
    }

    /**
     * One rung down the ladder, saying which rung and why. The exception is passed to the log
     * rather than swallowed — the cause of a persistent failure is the only thing that makes it
     * fixable.
     */
    private BigDecimal degradeFrom(Exception failure) {
        Quote cached = lastGood;
        if (cached != null && cached.isYoungerThan(properties.maxRateAge(), clock.instant())) {
            log.warn("USD/VND lookup failed; using the rate fetched at {} ({})",
                    cached.fetchedAt(), cached.rate(), failure);
            return cached.rate();
        }
        log.error("USD/VND lookup failed and no recent rate is cached; pricing this payment at "
                        + "the configured fallback of {} VND. Payments are being converted at a "
                        + "constant, not at market.",
                properties.fallbackUsdToVnd(), failure);
        return properties.fallbackUsdToVnd();
    }

    /** A rate and when it was fetched. */
    private record Quote(BigDecimal rate, Instant fetchedAt) {
        boolean isYoungerThan(Duration age, Instant now) {
            return fetchedAt.isAfter(now.minus(age));
        }
    }
}
