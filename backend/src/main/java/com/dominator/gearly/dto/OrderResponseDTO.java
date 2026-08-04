package com.dominator.gearly.dto;

import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.Payment;
import com.dominator.gearly.ordering.domain.ShippingInformation;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.UserId;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Public/admin response view of an {@link com.dominator.gearly.ordering.domain.Order}.
 * Field names and types mirror the aggregate exactly so the JSON on the wire is
 * unchanged; the point is to stop returning the persistence entity (with its
 * Mongo/audit annotations) straight from controllers.
 */
@Getter
@Setter
public class OrderResponseDTO {
    private String id;
    private UserId userId;
    private List<OrderLine> items;
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
