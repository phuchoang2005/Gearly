package com.dominator.gearly.catalog.api;

import com.dominator.gearly.catalog.domain.Image;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummaryDTO {
    private String id;
    private String title;
    private List<String> authors;
    private Money price;
    private int stock;
    private ProductCondition condition;
    private double averageRating;
    private int ratingCount;
    private int totalRating;
    private List<Image> images;
}

