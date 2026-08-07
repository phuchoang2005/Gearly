package com.dominator.gearly.analytics.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopSellerDTO {
    private String productId;
    private String title;
    private long totalSold;
}
