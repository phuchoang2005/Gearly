package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.Payment;
import com.dominator.gearly.ordering.domain.ShippingInformation;

import java.time.Instant;
import java.util.List;

/**
 * An administrator correcting individual fields of an order. A null component means "leave
 * that alone" — the {@code PATCH} semantics the endpoint has always had.
 */
public record AdminOrderPatchCommand(List<OrderLine> lines,
                                     ShippingInformation shippingInformation,
                                     Payment payment,
                                     OrderStatus orderStatus,
                                     Instant doneAt) {
}
