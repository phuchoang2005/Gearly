package com.dominator.gearly.payments.infrastructure;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The exchange-rate ladder: a fresh quote, else the last one this process fetched, else the
 * configured constant.
 *
 * <p>None of this was reachable before S13. {@code FxService} built its client in a field
 * initialiser and wrapped everything in {@code catch (Exception ignored)}, so the only
 * observable behaviour was "returns 23000 when offline" and there was no way to distinguish
 * that from "returns 23000 always".
 */
class OpenErApiExchangeRateProviderTest {

    private static final String URL = "https://rates.test/latest/USD";

    private static final FxProperties PROPERTIES = new FxProperties(
            URL, new BigDecimal("23000"), Duration.ofSeconds(5), Duration.ofHours(12));

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        clock = new MutableClock(Instant.parse("2026-08-07T00:00:00Z"));
    }

    private OpenErApiExchangeRateProvider provider() {
        return new OpenErApiExchangeRateProvider(builder.build(), PROPERTIES, clock);
    }

    @Test
    @DisplayName("a live quote is used as given")
    void usesTheLiveQuote() {
        server.expect(MockRestRequestMatchers.requestTo(URL))
                .andRespond(withSuccess("{\"rates\":{\"VND\":26314.5}}", MediaType.APPLICATION_JSON));

        assertThat(provider().usdToVnd()).isEqualByComparingTo("26314.5");
        server.verify();
    }

    @Test
    @DisplayName("with no cached rate, a failed lookup falls back to the configured constant")
    void fallsBackWhenNothingIsCached() {
        server.expect(MockRestRequestMatchers.requestTo(URL)).andRespond(withServerError());

        assertThat(provider().usdToVnd()).isEqualByComparingTo("23000");
    }

    /**
     * The rung that did not exist before. The old code went straight from "the call threw" to
     * "23000", so a transient outage repriced every payment at a constant that was already
     * years stale — roughly 12% under the real rate — with nothing logged.
     */
    @Test
    @DisplayName("a failed lookup prefers the last rate this process actually fetched")
    void prefersTheLastGoodRateOverTheFallback() {
        server.expect(once(), MockRestRequestMatchers.requestTo(URL))
                .andRespond(withSuccess("{\"rates\":{\"VND\":26000}}", MediaType.APPLICATION_JSON));
        server.expect(once(), MockRestRequestMatchers.requestTo(URL)).andRespond(withServerError());

        OpenErApiExchangeRateProvider provider = provider();
        assertThat(provider.usdToVnd()).isEqualByComparingTo("26000");

        clock.advance(Duration.ofHours(1));
        assertThat(provider.usdToVnd()).isEqualByComparingTo("26000");
        server.verify();
    }

    @Test
    @DisplayName("a cached rate older than max-rate-age is abandoned for the configured fallback")
    void staleCacheIsAbandoned() {
        server.expect(once(), MockRestRequestMatchers.requestTo(URL))
                .andRespond(withSuccess("{\"rates\":{\"VND\":26000}}", MediaType.APPLICATION_JSON));
        server.expect(once(), MockRestRequestMatchers.requestTo(URL)).andRespond(withServerError());

        OpenErApiExchangeRateProvider provider = provider();
        provider.usdToVnd();

        clock.advance(Duration.ofHours(13));
        assertThat(provider.usdToVnd()).isEqualByComparingTo("23000");
    }

    @Test
    @DisplayName("a 200 with no rates.VND field is a failure, not a zero rate")
    void missingFieldIsAFailure() {
        server.expect(MockRestRequestMatchers.requestTo(URL))
                .andRespond(withSuccess("{\"result\":\"error\"}", MediaType.APPLICATION_JSON));

        assertThat(provider().usdToVnd()).isEqualByComparingTo("23000");
    }

    /**
     * A rate of zero would make every order cost nothing in VND. The old code would have
     * returned it: {@code new BigDecimal("0")} parses fine and the {@code isEmpty()} check does
     * not catch it.
     */
    @Test
    @DisplayName("a non-positive quoted rate is refused rather than used")
    void nonPositiveRateIsRefused() {
        server.expect(MockRestRequestMatchers.requestTo(URL))
                .andRespond(withSuccess("{\"rates\":{\"VND\":0}}", MediaType.APPLICATION_JSON));

        assertThat(provider().usdToVnd()).isEqualByComparingTo("23000");
    }

    @Test
    @DisplayName("an unparseable rate is refused rather than throwing out of the checkout")
    void garbageRateIsRefused() {
        server.expect(MockRestRequestMatchers.requestTo(URL))
                .andRespond(withSuccess("{\"rates\":{\"VND\":\"n/a\"}}", MediaType.APPLICATION_JSON));

        assertThat(provider().usdToVnd()).isEqualByComparingTo("23000");
    }

    @Test
    @DisplayName("a recovered endpoint is used again immediately")
    void recoversOnTheNextCall() {
        server.expect(once(), MockRestRequestMatchers.requestTo(URL)).andRespond(withServerError());
        server.expect(once(), MockRestRequestMatchers.requestTo(URL))
                .andRespond(withSuccess("{\"rates\":{\"VND\":25500}}", MediaType.APPLICATION_JSON));

        OpenErApiExchangeRateProvider provider = provider();
        assertThat(provider.usdToVnd()).isEqualByComparingTo("23000");
        assertThat(provider.usdToVnd()).isEqualByComparingTo("25500");
        server.verify();
    }

    @Test
    @DisplayName("an HTTP error status is a failure even though a body came back")
    void errorStatusIsAFailure() {
        server.expect(MockRestRequestMatchers.requestTo(URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"rates\":{\"VND\":26000}}"));

        assertThat(provider().usdToVnd()).isEqualByComparingTo("23000");
    }

    /** A clock the test moves by hand, so cache expiry is asserted rather than waited for. */
    private static final class MutableClock extends Clock {
        private Instant now;

        private MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration by) {
            now = now.plus(by);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }
    }
}
