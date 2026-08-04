package com.dominator.gearly.ordering.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.math.BigDecimal;

/**
 * The order-pricing numbers, bound from {@code gearly.pricing.*}.
 *
 * <p>They were {@code private static final} fields on {@code CustomerOrderService}, which
 * meant changing the tax rate or the free-shipping threshold was a code change and a
 * redeploy, and that the two admin write paths computing their own totals could not see them.
 *
 * <p>The defaults reproduce those constants exactly, so an environment that sets none of
 * these prices orders the way it always has. They are declared here rather than only in
 * {@code application.properties} so the test profile and any future environment get the same
 * numbers without having to repeat them.
 */
@ConfigurationProperties("gearly.pricing")
public record PricingProperties(

        /** Sales tax applied to the line subtotal. 0.08 = 8%. */
        @DefaultValue("0.08") BigDecimal taxRate,

        /** A subtotal strictly greater than this ships free. */
        @DefaultValue("30.00") BigDecimal freeShippingThreshold,

        /** What shipping costs when the subtotal does not clear the threshold. */
        @DefaultValue("15.00") BigDecimal standardShippingCost) {
}
