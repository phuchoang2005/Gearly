package com.dominator.gearly.payments.infrastructure;

import com.dominator.gearly.payments.domain.ExchangeRateProvider;
import com.dominator.gearly.payments.domain.PaymentGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Wires this context's adapters and binds its configuration.
 *
 * <h2>Why the adapters are {@code @Bean}s and not {@code @Component}s</h2>
 * Both talk to a different third party with a different timeout budget, so each needs its own
 * {@link RestClient}. Two same-typed beans mean qualifiers on the constructors, which puts the
 * name of a bean inside the class that consumes it. Constructing them here instead keeps the
 * adapters plain objects — one constructor, no annotations, testable with {@code new} — and
 * puts the whole wiring decision on one screen.
 *
 * <p>This is also the plan's "inject {@code RestClient} as a bean instead of
 * {@code new RestTemplate()}" item, and the reason it was worth doing: a client that a class
 * constructs in a field initialiser cannot be replaced, so neither
 * {@code MomoService.createPaymentUrl} nor {@code FxService.getUsdToVndRate} was reachable by
 * any test. Both are now, through {@code MockRestServiceServer}.
 */
@Configuration
@EnableConfigurationProperties({MomoProperties.class, FxProperties.class})
public class PaymentsConfiguration {

    @Bean
    public PaymentGateway paymentGateway(MomoProperties momo,
                                         ExchangeRateProvider exchangeRates,
                                         ObjectMapper objectMapper,
                                         RestClient.Builder builder) {
        return new MomoPaymentGateway(momo, exchangeRates, objectMapper,
                clientWith(builder, momo.timeout()));
    }

    @Bean
    public ExchangeRateProvider exchangeRateProvider(FxProperties fx, RestClient.Builder builder) {
        return new OpenErApiExchangeRateProvider(clientWith(builder, fx.timeout()), fx);
    }

    /**
     * A client that gives up.
     *
     * <p>Neither predecessor had a timeout — {@code new RestTemplate()} has none — so a hung
     * third party hung the request thread indefinitely. Since S10 the gateway call is outside
     * the database transaction, so this is a stuck HTTP worker rather than a stuck transaction,
     * but a checkout that never answers is still a checkout that never answers.
     */
    private static RestClient clientWith(RestClient.Builder builder, Duration timeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return builder.clone().requestFactory(factory).build();
    }
}
