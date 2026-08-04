package com.dominator.gearly.dto;

import com.dominator.gearly.model.Image;
import com.dominator.gearly.shared.domain.CategoryId;
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
public class ProductUpdateDTO {
    private String title;
    private List<String> authors;
    private String description;
    private Money price;
    private Money originalPrice;
    private ProductCondition condition;
    private int stock;
    private List<CategoryId> categoryIds;
    private List<Image> images;
}

