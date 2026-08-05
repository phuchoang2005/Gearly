package com.dominator.gearly.catalog.api;

import com.dominator.gearly.catalog.domain.Product;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns {@link Product} aggregates into the response shapes the two frontends parse.
 *
 * <p>Was {@code mapper.ProductMapper}. Two things left with the move. The {@code toEntity} /
 * {@code updateEntity} half is gone: building an aggregate out of a request body one setter at
 * a time is exactly what {@code Product.create} and {@code Product.amend} exist to prevent, so
 * {@code AdminProductService} calls those and this class only maps outward. And
 * {@code copyImages} is gone with it — {@link com.dominator.gearly.catalog.domain.Image} is
 * immutable, so there is nothing to defend against.
 *
 * <p>The rating still leaves as three flat fields. The aggregate keeps them that way for the
 * query planner (see {@code Product}); the wire keeps them that way because both frontends
 * read {@code averageRating} and {@code ratingCount} directly.
 */
@Component
public class ProductResponseMapper {

    /** Compact catalog card used by search, wishlist, and best-seller listings. */
    public ProductSummaryDTO toSummaryDto(Product product) {
        return new ProductSummaryDTO(
                product.getId(),
                product.getTitle(),
                product.getAuthors(),
                product.getPrice(),
                product.getStock().toInt(),
                product.getCondition(),
                product.getAverageRating(),
                product.getRatingCount(),
                product.getTotalRating(),
                product.getImages()
        );
    }

    /** Full admin view; {@code categoryNames} is resolved by the caller. */
    public AdminProductDTO toAdminDto(Product product, List<String> categoryNames) {
        AdminProductDTO dto = new AdminProductDTO();
        dto.setId(product.getId());
        dto.setTitle(product.getTitle());
        dto.setAuthors(product.getAuthors());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setOriginalPrice(product.getOriginalPrice());
        dto.setCondition(product.getCondition());
        dto.setStock(product.getStock().toInt());
        dto.setCategoryIds(product.getCategoryIds());
        dto.setCategoryNames(categoryNames);
        dto.setImages(product.getImages());
        dto.setAverageRating(product.getAverageRating());
        dto.setRatingCount(product.getRatingCount());
        dto.setTotalRating(product.getTotalRating());
        return dto;
    }

    public ProductInLowStockDTO toLowStockDto(Product product) {
        return new ProductInLowStockDTO(product.getId(), product.getTitle(), product.getStock().toInt());
    }

    /**
     * Full product-detail response. {@code categoryNames} is resolved by
     * {@code CategoryNameProjection} and passed in — it used to be a {@code @Transient} field
     * on the entity that the read path filled and every other path left null.
     */
    public ProductResponseDTO toResponseDto(Product product, List<String> categoryNames) {
        ProductResponseDTO dto = new ProductResponseDTO();
        dto.setId(product.getId());
        dto.setTitle(product.getTitle());
        dto.setAuthors(product.getAuthors());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setOriginalPrice(product.getOriginalPrice());
        dto.setCondition(product.getCondition());
        dto.setStock(product.getStock().toInt());
        dto.setCategoryIds(product.getCategoryIds());
        dto.setImages(product.getImages());
        dto.setCategoryNames(categoryNames);
        dto.setAverageRating(product.getAverageRating());
        dto.setRatingCount(product.getRatingCount());
        dto.setTotalRating(product.getTotalRating());
        dto.setAddedAt(product.getAddedAt());
        dto.setModifiedAt(product.getModifiedAt());
        return dto;
    }
}
