package com.dominator.gearly.analytics.api;

import lombok.Data;

import java.util.List;

@Data
public class TopCategoryQuantityDTO {
    private String categoryId;
    private String categoryName;
    private List<ProductQuantityDTO> top10Products;

    @Data
    public static class ProductQuantityDTO {
        private String productId;
        private String title;
        private int totalQuantitySold;
    }
}
