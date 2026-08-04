package com.dominator.gearly.repository.custom;

import com.dominator.gearly.dto.ProductSummaryDTO;
import com.dominator.gearly.shared.domain.ProductCondition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductRepositoryCustom {
    Page<ProductSummaryDTO> findProducts(ProductCondition condition, double minPrice, double maxPrice,
                                   List<String> genres, String search, double minRating, Pageable pageable);
}
