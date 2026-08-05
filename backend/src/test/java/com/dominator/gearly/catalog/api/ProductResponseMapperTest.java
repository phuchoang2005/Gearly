package com.dominator.gearly.catalog.api;

import com.dominator.gearly.catalog.domain.Image;
import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductFixture;
import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The outward half of what {@code ProductMapperTest} covered. The inward half —
 * {@code toEntity} and {@code updateEntity} — has no counterpart here because it has no
 * counterpart in production: building an aggregate a setter at a time is what
 * {@code Product.create} and {@code Product.amend} replaced, and {@code ProductTest} asserts
 * those directly.
 */
class ProductResponseMapperTest {

    private final ProductResponseMapper mapper = new ProductResponseMapper();

    @Test
    void summaryAdminAndLowStock_dtos() {
        CategoryId category = CategoryId.of(new ObjectId().toHexString());
        Product product = ProductFixture.aProduct()
                .withId("b1")
                .titled("Case")
                .by("Brand")
                .pricedAt(80.0)
                .withStock(7)
                .inCondition(ProductCondition.NEW)
                .rated(4, 4)
                .withImages(new Image("u", "a"))
                .inCategories(category)
                .build();

        ProductSummaryDTO summary = mapper.toSummaryDto(product);
        assertThat(summary.getId()).isEqualTo("b1");
        assertThat(summary.getTitle()).isEqualTo("Case");
        assertThat(summary.getPrice()).isEqualTo(Money.of(80.0));
        assertThat(summary.getStock()).isEqualTo(7);
        assertThat(summary.getImages()).hasSize(1);

        AdminProductDTO admin = mapper.toAdminDto(product, List.of("Cases"));
        assertThat(admin.getId()).isEqualTo("b1");
        assertThat(admin.getCategoryIds()).containsExactly(category);
        assertThat(admin.getCategoryNames()).containsExactly("Cases");
        assertThat(admin.getTotalRating()).isEqualTo(8);
        assertThat(admin.getAverageRating()).isEqualTo(4.0);

        ProductInLowStockDTO low = mapper.toLowStockDto(product);
        assertThat(low.getId()).isEqualTo("b1");
        assertThat(low.getTitle()).isEqualTo("Case");
        assertThat(low.getStock()).isEqualTo(7);
    }

    @Test
    void responseDto_carriesTheResolvedCategoryNames() {
        CategoryId category = CategoryId.of(new ObjectId().toHexString());
        Product product = ProductFixture.aProduct()
                .persistedAs("p1", Instant.parse("2026-01-02T03:04:05Z"),
                        Instant.parse("2026-01-03T03:04:05Z"))
                .titled("RTX 4090")
                .by("NVIDIA")
                .described("flagship GPU")
                .pricedAt(1599.0)
                .originallyPricedAt(1799.0)
                .withStock(5)
                .inCategories(category)
                .withImages(new Image("http://img/a.png", "gpu"))
                .rated(5, 4)
                .build();

        ProductResponseDTO dto = mapper.toResponseDto(product, List.of("Graphics Cards"));

        assertThat(dto.getId()).isEqualTo("p1");
        assertThat(dto.getTitle()).isEqualTo("RTX 4090");
        assertThat(dto.getPrice()).isEqualTo(Money.of(1599.0));
        assertThat(dto.getOriginalPrice()).isEqualTo(Money.of(1799.0));
        assertThat(dto.getStock()).isEqualTo(5);
        assertThat(dto.getCategoryIds()).containsExactly(category);
        // The names arrive from the projection now; they used to be a @Transient field the
        // read path populated on the aggregate and every other path left null.
        assertThat(dto.getCategoryNames()).containsExactly("Graphics Cards");
        assertThat(dto.getRatingCount()).isEqualTo(2);
        assertThat(dto.getAddedAt()).isEqualTo(Instant.parse("2026-01-02T03:04:05Z"));
        assertThat(dto.getModifiedAt()).isEqualTo(Instant.parse("2026-01-03T03:04:05Z"));
    }
}
