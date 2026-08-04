package com.dominator.gearly.ordering.api;

import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.Payment;
import com.dominator.gearly.ordering.domain.ShippingInformation;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/** Admin partial-update payload for an order. An absent field leaves that field alone. */
@Getter
@Setter
@NoArgsConstructor
public class OrderPatchDTO {
    private OrderStatus orderStatus;
    private ShippingInformation shippingInformation;
    private Payment payment;
    private List<OrderLine> items;
    private Instant doneAt;
}
