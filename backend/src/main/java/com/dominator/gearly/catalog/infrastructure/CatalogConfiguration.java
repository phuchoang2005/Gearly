package com.dominator.gearly.catalog.infrastructure;

import com.dominator.gearly.catalog.domain.LowStockThreshold;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires this context's domain values from configuration. Mirrors {@code OrderingConfiguration}:
 * a domain class may carry no Spring annotations, so the binding from {@code gearly.catalog.*}
 * to a constructor call happens out here, in the layer that is allowed to know about the
 * framework.
 */
@Configuration
@EnableConfigurationProperties(CatalogProperties.class)
public class CatalogConfiguration {

    @Bean
    public LowStockThreshold lowStockThreshold(CatalogProperties properties) {
        return LowStockThreshold.of(properties.lowStockThreshold());
    }
}
