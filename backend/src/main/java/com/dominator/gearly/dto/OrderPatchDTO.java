// src/main/java/com/dominator/gearly/dto/OrderPatchDTO.java
package com.dominator.gearly.dto;

import com.dominator.gearly.model.*;
import com.dominator.gearly.ordering.domain.OrderStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class OrderPatchDTO {
    private OrderStatus orderStatus;
    private ShippingInformation shippingInformation;
    private Payment payment;
    private List<OrderItem> items;
    private Instant doneAt;
}
