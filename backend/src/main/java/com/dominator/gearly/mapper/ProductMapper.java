package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.AdminProductDTO;
import com.dominator.gearly.dto.ProductCreateDTO;
import com.dominator.gearly.dto.ProductInLowStockDTO;
import com.dominator.gearly.dto.ProductSummaryDTO;
import com.dominator.gearly.dto.ProductUpdateDTO;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.Image;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Maps {@link Product} entities to catalog/admin DTOs and builds entities from
 * create/update requests. Category-name resolution stays in the service layer;
 * callers pass the resolved names into {@link #toAdminDto(Product, List)}.
 */
@Component
public class ProductMapper {

    /** Compact catalog card used by search, wishlist, and best-seller listings. */
    public ProductSummaryDTO toSummaryDto(Product product) {
        return new ProductSummaryDTO(
                product.getId(),
                product.getTitle(),
                product.getAuthors(),
                product.getPrice(),
                product.getStock(),
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
        dto.setStock(product.getStock());
        dto.setCategoryIds(product.getCategoryIds());
        dto.setCategoryNames(categoryNames);
        dto.setImages(product.getImages());
        dto.setAverageRating(product.getAverageRating());
        dto.setRatingCount(product.getRatingCount());
        dto.setTotalRating(product.getTotalRating());
        return dto;
    }

    public ProductInLowStockDTO toLowStockDto(Product product) {
        return new ProductInLowStockDTO(product.getId(), product.getTitle(), product.getStock());
    }

    /** Builds a new entity from a create request. Rating fields and timestamps stay with the service. */
    public Product toEntity(ProductCreateDTO dto) {
        Product product = new Product();
        product.setTitle(dto.getTitle());
        product.setAuthors(dto.getAuthors());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setCondition(dto.getCondition());
        product.setStock(dto.getStock());
        product.setCategoryIds(dto.getCategoryIds());
        product.setImages(copyImages(dto.getImages()));
        return product;
    }

    /** Applies an update request onto an existing entity, preserving id, ratings, and timestamps. */
    public void updateEntity(Product product, ProductUpdateDTO dto) {
        product.setTitle(dto.getTitle());
        product.setAuthors(dto.getAuthors());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setOriginalPrice(dto.getOriginalPrice());
        product.setCondition(dto.getCondition());
        product.setStock(dto.getStock());
        product.setImages(dto.getImages());
        // The update DTO carries category ids as hex strings; store them as ObjectId,
        // consistent with create and the read path (Product.categoryIds is List<ObjectId>).
        product.setCategoryIds(toObjectIds(dto.getCategoryIds()));
    }

    private List<Image> copyImages(List<Image> images) {
        if (images == null) {
            return null;
        }
        return images.stream()
                .map(i -> new Image(i.getUrl(), i.getAlt()))
                .collect(Collectors.toList());
    }

    private List<ObjectId> toObjectIds(List<String> ids) {
        if (ids == null) {
            return null;
        }
        return ids.stream().map(ObjectId::new).collect(Collectors.toList());
    }
}
