// src/main/java/com/dominator/bookify/dto/OrderPatchDTO.java
package com.dominator.bookify.dto;

import com.dominator.bookify.model.*;
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
