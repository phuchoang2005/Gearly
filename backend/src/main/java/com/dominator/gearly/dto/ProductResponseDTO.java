package com.dominator.gearly.dto;

import com.dominator.gearly.model.Image;
import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Full product-detail response (GET /api/products/{id}). Mirrors the {@link
 * com.dominator.gearly.model.Product} entity field-for-field, including the
 * resolved {@code categoryNames}, so the wire shape is unchanged while the
 * controller no longer hands back the persistence entity.
 */
@Getter
@Setter
public class ProductResponseDTO {
    private String id;
    private String title;
    private List<String> authors;
    private String description;
    private Money price;
    private Money originalPrice;
    private ProductCondition condition;
    private int stock;
    private List<CategoryId> categoryIds;
    private List<Image> images;
    private List<String> categoryNames;
    private double averageRating;
    private int ratingCount;
    private int totalRating;
    private Instant addedAt;
    private Instant modifiedAt;
}
