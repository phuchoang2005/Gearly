package com.dominator.gearly.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingTest {

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void acceptsOneThroughFive(int stars) {
        assertThat(new Rating(stars).value()).isEqualTo(stars);
    }

    /**
     * {@code 900} is the value the S8 characterization suite pins as being accepted today
     * and folded permanently into the product's average. This is the type that makes it
     * unrepresentable; S12 is where {@code Review.rating} adopts it.
     */
    @ParameterizedTest
    @ValueSource(ints = {0, 6, -1, 900, Integer.MAX_VALUE})
    void rejectsAnythingOutsideOneThroughFive(int stars) {
        assertThatThrownBy(() -> new Rating(stars))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be between 1 and 5");
    }

    @Test
    void serializesAsABareInt() {
        assertThat(Rating.of(4).toInt()).isEqualTo(4);
    }

    @Test
    void ordersByStars() {
        assertThat(Rating.of(5)).isGreaterThan(Rating.of(3));
    }
}
