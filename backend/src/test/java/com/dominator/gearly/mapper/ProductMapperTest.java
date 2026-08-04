package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.AdminProductDTO;
import com.dominator.gearly.dto.ProductCreateDTO;
import com.dominator.gearly.dto.ProductInLowStockDTO;
import com.dominator.gearly.dto.ProductSummaryDTO;
import com.dominator.gearly.dto.ProductUpdateDTO;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.Image;
import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void toEntity_copiesFields_andRewrapsImages() {
        CategoryId cat = CategoryId.of(new ObjectId().toHexString());
        Image img = new Image("http://img/1.png", "gpu");

        ProductCreateDTO dto = new ProductCreateDTO();
        dto.setTitle("RTX 4090");
        dto.setAuthors(List.of("NVIDIA"));
        dto.setDescription("GPU");
        dto.setPrice(Money.of(1599.0));
        dto.setOriginalPrice(Money.of(1799.0));
        dto.setCondition(ProductCondition.NEW);
        dto.setStock(5);
        dto.setCategoryIds(List.of(cat));
        dto.setImages(List.of(img));

        Product product = mapper.toEntity(dto);

        assertThat(product.getTitle()).isEqualTo("RTX 4090");
        assertThat(product.getAuthors()).containsExactly("NVIDIA");
        assertThat(product.getPrice()).isEqualTo(Money.of(1599.0));
        assertThat(product.getOriginalPrice()).isEqualTo(Money.of(1799.0));
        assertThat(product.getStock()).isEqualTo(5);
        assertThat(product.getCategoryIds()).containsExactly(cat);
        assertThat(product.getImages()).hasSize(1);
        assertThat(product.getImages().get(0)).isNotSameAs(img); // defensively re-wrapped
        assertThat(product.getImages().get(0).getUrl()).isEqualTo("http://img/1.png");
        assertThat(product.getImages().get(0).getAlt()).isEqualTo("gpu");
    }

    @Test
    void updateEntity_updatesFields_preservesIdAndRatings_convertsCategoryIds() {
        Product product = new Product();
        product.setId("b1");
        product.setAverageRating(4.5);
        product.setRatingCount(10);
        product.setTotalRating(45);
        product.setAddedAt(Instant.parse("2025-01-01T00:00:00Z"));

        String hex = new ObjectId().toHexString();
        ProductUpdateDTO dto = new ProductUpdateDTO();
        dto.setTitle("New Title");
        dto.setAuthors(List.of("A"));
        dto.setDescription("d");
        dto.setPrice(Money.of(10.0));
        dto.setOriginalPrice(Money.of(12.0));
        dto.setCondition(ProductCondition.GOOD);
        dto.setStock(3);
        dto.setImages(List.of(new Image("u", "a")));
        dto.setCategoryIds(List.of(CategoryId.of(hex)));

        mapper.updateEntity(product, dto);

        // preserved
        assertThat(product.getId()).isEqualTo("b1");
        assertThat(product.getAverageRating()).isEqualTo(4.5);
        assertThat(product.getRatingCount()).isEqualTo(10);
        assertThat(product.getTotalRating()).isEqualTo(45);
        assertThat(product.getAddedAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
        // updated
        assertThat(product.getTitle()).isEqualTo("New Title");
        assertThat(product.getCondition()).isEqualTo(ProductCondition.GOOD);
        assertThat(product.getStock()).isEqualTo(3);
        // The update DTO and the entity now share one CategoryId type; the hex-string ->
        // ObjectId conversion that used to live in the mapper is the converter's job.
        assertThat(product.getCategoryIds()).containsExactly(CategoryId.of(hex));
    }

    @Test
    void summaryAdminAndLowStock_dtos() {
        CategoryId cat = CategoryId.of(new ObjectId().toHexString());
        Product product = new Product();
        product.setId("b1");
        product.setTitle("Case");
        product.setAuthors(List.of("Brand"));
        product.setPrice(Money.of(80.0));
        product.setStock(7);
        product.setCondition(ProductCondition.NEW);
        product.setAverageRating(4.0);
        product.setRatingCount(2);
        product.setTotalRating(8);
        product.setImages(List.of(new Image("u", "a")));
        product.setCategoryIds(List.of(cat));

        ProductSummaryDTO summary = mapper.toSummaryDto(product);
        assertThat(summary.getId()).isEqualTo("b1");
        assertThat(summary.getTitle()).isEqualTo("Case");
        assertThat(summary.getPrice()).isEqualTo(Money.of(80.0));
        assertThat(summary.getImages()).hasSize(1);

        AdminProductDTO admin = mapper.toAdminDto(product, List.of("Cases"));
        assertThat(admin.getId()).isEqualTo("b1");
        assertThat(admin.getCategoryIds()).containsExactly(cat);
        assertThat(admin.getCategoryNames()).containsExactly("Cases");
        assertThat(admin.getTotalRating()).isEqualTo(8);

        ProductInLowStockDTO low = mapper.toLowStockDto(product);
        assertThat(low.getId()).isEqualTo("b1");
        assertThat(low.getTitle()).isEqualTo("Case");
        assertThat(low.getStock()).isEqualTo(7);
    }
}
