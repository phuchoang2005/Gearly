package com.dominator.gearly.reviews.api;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductReviewsDTO {
    String productId;
    int pageSize;
    int pageIndex;
    int rating = 0;
    String sortBy = "addedAt";
}
