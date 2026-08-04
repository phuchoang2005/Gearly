package com.dominator.gearly.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypedIdTest {

    private static final String HEX = "682023424a1ae581e0445357";

    @Test
    void carriesTheStringItWasBuiltFrom() {
        assertThat(new ProductId(HEX).value()).isEqualTo(HEX);
        assertThat(new ProductId(HEX).toString()).isEqualTo(HEX);
    }

    @Test
    void trimsIncidentalWhitespace() {
        assertThat(OrderId.of("  o1  ").value()).isEqualTo("o1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankIds(String blank) {
        assertThatThrownBy(() -> new ProductId(blank))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void rejectsNullIds() {
        assertThatThrownBy(() -> new UserId(null)).isInstanceOf(NullPointerException.class);
    }

    /**
     * The reason the types exist: two ids that are both {@code String} underneath are
     * mutually assignable, and nothing in {@code ReviewService} or
     * {@code applyStockAndClearCart} would notice them being swapped.
     */
    @Test
    void idsOfDifferentKindsAreNotEqualEvenWithTheSameValue() {
        assertThat((Object) new ProductId(HEX)).isNotEqualTo(new OrderId(HEX));
        assertThat((Object) new UserId("u1")).isNotEqualTo(new CartId("u1"));
    }

    @Test
    void idsOfTheSameKindCompareByValue() {
        assertThat(new ReviewId("r1")).isEqualTo(new ReviewId("r1"));
        assertThat(new ReviewId("r1")).hasSameHashCodeAs(new ReviewId("r1"));
    }

    /**
     * {@link CategoryId} is stored as a BSON {@code ObjectId}, so a value that cannot
     * become one has to fail at construction rather than at write time.
     */
    @Test
    void categoryIdRequiresAnObjectIdShapedValue() {
        assertThat(new CategoryId(HEX).value()).isEqualTo(HEX);

        assertThatThrownBy(() -> new CategoryId("not-an-object-id"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("24-character hex ObjectId");
        assertThatThrownBy(() -> new CategoryId("682023424a1ae581e044535"))  // 23 chars
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("24-character hex ObjectId");
    }

    @Test
    void otherIdsDoNotRequireTheObjectIdShape() {
        // order and user ids are stored as plain strings and are not all ObjectId hex
        assertThat(new OrderId("Gearly-123").value()).isEqualTo("Gearly-123");
    }
}
