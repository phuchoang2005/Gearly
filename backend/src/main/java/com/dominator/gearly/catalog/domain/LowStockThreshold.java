package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.Quantity;

/**
 * When the catalog considers a product to be running out.
 *
 * <p>This number spent its life as the literal {@code 10} inside
 * {@code @Query("{'stock': {$lt: 10}}")} on a one-method Spring Data interface — a decision
 * about what a merchant wants to be warned about, expressed as part of a query string, where
 * it could be neither configured nor tested at any other value.
 *
 * <p>It is a domain type rather than a bare {@code int} for the same reason
 * {@code PricingPolicy} is: the application layer should be handed the rule, not the
 * configuration file the rule was read out of. {@code CatalogConfiguration} does the binding,
 * because a domain class may carry no Spring annotations.
 */
public record LowStockThreshold(Quantity value) {

    public LowStockThreshold {
        if (value == null) {
            throw new IllegalArgumentException("a low-stock threshold must have a value");
        }
    }

    public static LowStockThreshold of(int units) {
        return new LowStockThreshold(Quantity.of(units));
    }

    public boolean isLow(Quantity stock) {
        return stock.isLessThan(value);
    }
}
