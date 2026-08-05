package com.dominator.gearly.catalog.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProductInLowStockDTO {
    private String id;
    private String title;
    private int stock;
}
