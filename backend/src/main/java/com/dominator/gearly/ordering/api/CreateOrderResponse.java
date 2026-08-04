package com.dominator.gearly.ordering.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateOrderResponse {
    private String orderId;
    private String payUrl;
}
