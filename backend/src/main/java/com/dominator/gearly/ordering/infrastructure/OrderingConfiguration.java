package com.dominator.gearly.ordering.infrastructure;

import com.dominator.gearly.ordering.domain.PricingPolicy;
import com.dominator.gearly.shared.domain.Money;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires this context's domain services from configuration.
 *
 * <p>{@link PricingPolicy} is a plain object with no Spring annotations — a domain package
 * may not carry {@code @Service} or {@code @Component}, which ArchUnit enforces — so the
 * binding from {@code gearly.pricing.*} to a constructor call happens out here, in the layer
 * that is allowed to know about the framework.
 */
@Configuration
@EnableConfigurationProperties(PricingProperties.class)
public class OrderingConfiguration {

    @Bean
    public PricingPolicy pricingPolicy(PricingProperties properties) {
        return new PricingPolicy(
                properties.taxRate(),
                Money.of(properties.freeShippingThreshold()),
                Money.of(properties.standardShippingCost()));
    }
}
