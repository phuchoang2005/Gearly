package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.Payment;
import com.dominator.gearly.ordering.domain.ShippingInformation;
import com.dominator.gearly.shared.domain.UserId;

import java.time.Instant;
import java.util.List;

/**
 * An administrator creating or wholly replacing an order.
 *
 * <p>There is no {@code totalAmount}. The old payload carried one and it was assigned
 * straight through, so an admin request could store lines worth $10 against a total of
 * $10,000. The total is derived from the lines by {@code PricingPolicy}; leaving the field off
 * this record is what makes that structural rather than a check someone has to remember. The
 * wire DTO still accepts the property so the admin frontend keeps working — it is documented
 * there as ignored, and it stops at the api layer.
 */
public record AdminOrderCommand(UserId userId,
                                List<OrderLine> lines,
                                ShippingInformation shippingInformation,
                                Payment payment,
                                OrderStatus orderStatus,
                                boolean reviewed,
                                String note,
                                Instant doneAt) {
}
