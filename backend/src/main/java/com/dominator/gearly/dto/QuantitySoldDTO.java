package com.dominator.gearly.dto;

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
