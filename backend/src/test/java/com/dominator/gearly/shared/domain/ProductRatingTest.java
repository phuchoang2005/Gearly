package com.dominator.gearly.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductRatingTest {

    @Test
    void anUnratedProductAveragesZero() {
        assertThat(ProductRating.NONE.average()).isZero();
        assertThat(ProductRating.NONE.isUnrated()).isTrue();
    }

    /**
     * Adopting this type must not move a single stored average, so the arithmetic is the
     * existing {@code ReviewService.applyRating} formula verbatim — including its
     * round-to-two-decimals step.
     */
    @Test
    void reproducesTheExistingAverageArithmetic() {
        // the seed's first product: 12 ratings totalling 54
        ProductRating seeded = new ProductRating(12, 54);
        assertThat(seeded.average()).isEqualTo(4.5);

        // a total that does not divide evenly is rounded to two decimals, not truncated
        assertThat(new ProductRating(3, 10).average()).isEqualTo(3.33);
        assertThat(new ProductRating(3, 11).average()).isEqualTo(3.67);
    }

    @Test
    void addingARatingUpdatesCountAndTotalTogether() {
        ProductRating rolled = ProductRating.NONE.add(Rating.of(5)).add(Rating.of(4));

        assertThat(rolled.count()).isEqualTo(2);
        assertThat(rolled.total()).isEqualTo(9);
        assertThat(rolled.average()).isEqualTo(4.5);
    }

    @Test
    void removingARatingReversesIt() {
        ProductRating rolled = ProductRating.NONE.add(Rating.of(5)).add(Rating.of(1));

        assertThat(rolled.remove(Rating.of(1))).isEqualTo(new ProductRating(1, 5));
    }

    @Test
    void rejectsATotalThatNoSetOfRatingsCouldProduce() {
        // three reviews can total at most 15 and at least 3
        assertThatThrownBy(() -> new ProductRating(3, 16))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("impossible");
        assertThatThrownBy(() -> new ProductRating(3, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("impossible");
    }

    @Test
    void rejectsATotalWithoutAnyRatings() {
        assertThatThrownBy(() -> new ProductRating(0, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("needs at least one rating");
    }

    @Test
    void rejectsNegativeCounts() {
        assertThatThrownBy(() -> new ProductRating(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
