package com.dominator.gearly.dto;

import com.dominator.gearly.model.OrderItem;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.model.Payment;
import com.dominator.gearly.model.ShippingInformation;
import com.dominator.gearly.shared.domain.Money;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Public/admin response view of an {@link com.dominator.gearly.model.Order}.
 * Field names and types mirror the entity exactly so the JSON on the wire is
 * unchanged; the point is to stop returning the mutable persistence entity
 * (with its Mongo/audit annotations) straight from controllers.
 */
@Getter
@Setter
public class OrderResponseDTO {
    private String id;
    private String userId;
    private List<OrderItem> items;
    private Money totalAmount;
    private Payment payment;
    private OrderStatus orderStatus;
    private ShippingInformation shippingInformation;
    private boolean isReviewed;
    private String note;
    private Instant addedAt;
    private Instant modifiedAt;
    private Instant doneAt;
}
