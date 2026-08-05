package com.dominator.gearly.catalog.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Catalog settings, bound from {@code gearly.catalog.*}. The counterpart of
 * {@code PricingProperties}, introduced for the same reason.
 *
 * <p>One value so far, and it is here because it was previously inside a Spring Data
 * annotation: {@code ProductsInStockRepository} declared
 * {@code @Query("{'stock': {$lt: 10}}")}. Ten is a decision about when a merchant wants to be
 * warned, not a fact about how the query is written, and where it sat it was unreachable from
 * configuration and impossible to test at any other value.
 *
 * <p>The default reproduces the constant exactly, so an environment that sets nothing shows
 * the dashboard it always showed.
 */
@ConfigurationProperties("gearly.catalog")
public record CatalogProperties(

        /** Fewer than this many units left counts as low stock on the admin dashboard. */
        @DefaultValue("10") int lowStockThreshold) {
}
