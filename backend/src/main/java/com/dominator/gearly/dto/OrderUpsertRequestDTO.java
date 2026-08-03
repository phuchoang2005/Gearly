package com.dominator.gearly.dto;

import com.dominator.gearly.model.OrderItem;
import com.dominator.gearly.model.OrderStatus;
import com.dominator.gearly.model.Payment;
import com.dominator.gearly.model.ShippingInformation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Admin create/update payload for an order. Replaces binding the raw {@link
 * com.dominator.gearly.model.Order} persistence entity directly to the request
 * body: the managed fields (id and the audit timestamps) are intentionally
 * absent so a client can never set them.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpsertRequestDTO {
    private String userId;
    private List<OrderItem> items;
    private double totalAmount;
    private Payment payment;
    private OrderStatus orderStatus;
    private ShippingInformation shippingInformation;
    private boolean reviewed;
    private String note;
    private Instant doneAt;
}
