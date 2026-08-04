package com.dominator.gearly.dto;

import com.dominator.gearly.shared.domain.Money;
import lombok.Data;

import java.util.List;

/**
 * Top-selling products for the admin dashboard. Populated by a Mongo aggregation
 * projection rather than by a mapper, so {@code price} is filled by the same
 * {@code DomainTypeConverters} pair the entities use — the projected BSON double becomes a
 * {@link Money}, and Jackson writes it back out as the double the dashboard already reads.
 */
@Data
public class BestSellerDTO {
    private String productId;
    private String title;
    private List<String> authors;
    private Money price;
    private int totalSold;
}
