package com.dominator.gearly.analytics.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuantitySoldDTO {
    private String productId;
    private long totalSold;
    private String title;
}
