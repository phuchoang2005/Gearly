package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.Rating;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The catalog aggregate's own rules, exercised with a plain constructor call and no Spring
 * context — which is the property the layer rules exist to preserve.
 *
 * <p>Most of what is asserted here used to be spread across {@code ProductService},
 * {@code CartService}, {@code CustomerOrderService}, {@code AdminProductService} and
 * {@code ReviewService}, in some cases in more than one of them at once. Testing it in one
 * place is the visible half of collapsing it into one place.
 */
class ProductTest {

    // ---- stock --------------------------------------------------------------

    @Nested
    @DisplayName("reserve / restock — the one stock rule")
    class Stock {

        @Test
        @DisplayName("reserving takes the units off the shelf")
        void reserveDecrementsStock() {
            Product product = ProductFixture.aProduct().withStock(5).build();

            product.reserve(Quantity.of(3));

            assertThat(product.getStock()).isEqualTo(Quantity.of(2));
        }

        @Test
        @DisplayName("reserving exactly what is left is allowed and empties the shelf")
        void reserveAllIsAllowed() {
            Product product = ProductFixture.aProduct().withStock(3).build();

            product.reserve(Quantity.of(3));

            assertThat(product.getStock()).isEqualTo(Quantity.ZERO);
            assertThat(product.isInStock()).isFalse();
        }

        @Test
        @DisplayName("reserving more than exists is refused, and nothing is taken")
        void reserveBeyondStockIsRefused() {
            Product product = ProductFixture.aProduct().titled("RTX 4090").withStock(2).build();

            assertThatThrownBy(() -> product.reserve(Quantity.of(3)))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("Only 2 left for \"RTX 4090\"!");

            assertThat(product.getStock()).as("a refused reservation must not move stock")
                    .isEqualTo(Quantity.of(2));
        }

        @Test
        @DisplayName("an out-of-stock product says so rather than reporting zero left")
        void outOfStockHasItsOwnMessage() {
            Product product = ProductFixture.aProduct().titled("RTX 4090").withStock(0).build();

            assertThatThrownBy(() -> product.reserve(Quantity.ONE))
                    .isInstanceOf(InsufficientStockException.class)
                    .hasMessage("\"RTX 4090\" is out of stock!");
        }

        @Test
        @DisplayName("assertCanSupply asks the same question without taking anything")
        void assertCanSupplyDoesNotMutate() {
            Product product = ProductFixture.aProduct().withStock(4).build();

            product.assertCanSupply(Quantity.of(4));

            assertThat(product.getStock()).isEqualTo(Quantity.of(4));
        }

        @Test
        @DisplayName("restocking puts cancelled units back")
        void restockAddsUnits() {
            Product product = ProductFixture.aProduct().withStock(1).build();

            product.restock(Quantity.of(3));

            assertThat(product.getStock()).isEqualTo(Quantity.of(4));
        }

        @Test
        @DisplayName("reserve then restock returns the product to where it started")
        void reserveAndRestockRoundTrip() {
            Product product = ProductFixture.aProduct().withStock(7).build();

            product.reserve(Quantity.of(4));
            product.restock(Quantity.of(4));

            assertThat(product.getStock()).isEqualTo(Quantity.of(7));
        }
    }

    // ---- rating -------------------------------------------------------------

    @Nested
    @DisplayName("addRating — the rollup that used to live in ReviewService")
    class RatingRollup {

        @Test
        @DisplayName("the first rating seeds count, total and average")
        void firstRatingSeedsTheAverage() {
            Product product = ProductFixture.aProduct().build();

            product.addRating(Rating.of(5));

            assertThat(product.getRatingCount()).isEqualTo(1);
            assertThat(product.getTotalRating()).isEqualTo(5);
            assertThat(product.getAverageRating()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("a subsequent rating folds into the running total")
        void subsequentRatingUpdatesTheAverage() {
            Product product = ProductFixture.aProduct().rated(5).build();

            product.addRating(Rating.of(4));

            assertThat(product.getRatingCount()).isEqualTo(2);
            assertThat(product.getTotalRating()).isEqualTo(9);
            assertThat(product.getAverageRating()).isEqualTo(4.5);
        }

        @Test
        @DisplayName("the average keeps the two-decimal rounding ReviewService.applyRating used")
        void averageIsRoundedToTwoDecimals() {
            // 5 + 4 + 4 = 13 over 3 -> 4.3333...
            Product product = ProductFixture.aProduct().rated(5, 4).build();

            product.addRating(Rating.of(4));

            assertThat(product.getAverageRating()).isEqualTo(4.33);
        }

        @Test
        @DisplayName("the three fields cannot drift apart — the average is always derived")
        void theRollupStaysConsistent() {
            Product product = ProductFixture.aProduct().rated(1, 2, 3, 4, 5).build();

            assertThat(product.rating().count()).isEqualTo(5);
            assertThat(product.rating().total()).isEqualTo(15);
            assertThat(product.getAverageRating()).isEqualTo(product.rating().average());
        }

        @Test
        @DisplayName("removing a rating is the exact inverse — the seam S12's ReviewRejected needs")
        void removeRatingIsTheInverse() {
            Product product = ProductFixture.aProduct().rated(5, 1).build();

            product.removeRating(Rating.of(1));

            assertThat(product.getRatingCount()).isEqualTo(1);
            assertThat(product.getTotalRating()).isEqualTo(5);
            assertThat(product.getAverageRating()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("an out-of-range star count cannot reach the rollup at all")
        void ratingIsBounded() {
            // The S8 suite pinned 900 stars being folded in and wrecking the average. The
            // value object is what makes that unrepresentable now; ReviewService turns the
            // rejection into a 400 rather than letting it surface as a 500.
            assertThatThrownBy(() -> Rating.of(900)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Rating.of(0)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ---- snapshot -----------------------------------------------------------

    @Nested
    @DisplayName("snapshot — what downstream contexts are allowed to see")
    class Snapshot {

        @Test
        @DisplayName("carries the seven published fields and nothing else")
        void carriesThePublishedFields() {
            Product product = ProductFixture.aProduct()
                    .withId("p1")
                    .titled("RTX 4090")
                    .by("NVIDIA", "Founders Edition")
                    .pricedAt(1599.0)
                    .inCondition(ProductCondition.LIKE_NEW)
                    .withStock(5)
                    .withImages(new Image("http://img/a.png", "gpu"),
                            new Image("http://img/b.png", "box"))
                    .build();

            CatalogSnapshot snapshot = product.snapshot();

            assertThat(snapshot.productId().value()).isEqualTo("p1");
            assertThat(snapshot.title()).isEqualTo("RTX 4090");
            assertThat(snapshot.author()).as("the first author, as the cart has always shown")
                    .isEqualTo("NVIDIA");
            assertThat(snapshot.price()).isEqualTo(Money.of(1599.0));
            assertThat(snapshot.imageUrl()).isEqualTo("http://img/a.png");
            assertThat(snapshot.condition()).isEqualTo(ProductCondition.LIKE_NEW);
            assertThat(snapshot.stock()).isEqualTo(Quantity.of(5));
        }

        @Test
        @DisplayName("an image-less product yields a null image rather than throwing")
        void imagelessProductDoesNotThrow() {
            // This is the OrderMapper.getImages().getFirst() crash. It took down checkout for
            // any product without a picture; CartMapper guarded and OrderMapper did not.
            Product product = ProductFixture.aProduct().withId("p1").withoutImages().build();

            assertThatCode(product::snapshot).doesNotThrowAnyException();
            assertThat(product.snapshot().imageUrl()).isNull();
        }

        @Test
        @DisplayName("an author-less product falls back to \"Unknown\", as CartMapper always did")
        void authorlessProductFallsBack() {
            Product product = ProductFixture.aProduct().withId("p1").withoutAuthors().build();

            assertThat(product.snapshot().author()).isEqualTo("Unknown");
        }
    }

    // ---- administration -----------------------------------------------------

    @Nested
    @DisplayName("create / amend")
    class Administration {

        @Test
        @DisplayName("a new product opens unrated")
        void newProductIsUnrated() {
            Product product = Product.create("Case", List.of("Brand"), "d", Money.of(80.0),
                    Money.of(90.0), ProductCondition.NEW, Quantity.of(7), null, null);

            assertThat(product.getRatingCount()).isZero();
            assertThat(product.getTotalRating()).isZero();
            assertThat(product.getAverageRating()).isZero();
            assertThat(product.rating().isUnrated()).isTrue();
            assertThat(product.getAddedAt()).isNotNull();
            assertThat(product.getModifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("amending replaces the description but never the rating or the identity")
        void amendPreservesRatingsAndIdentity() {
            CategoryId category = CategoryId.of(new ObjectId().toHexString());
            Product product = ProductFixture.aProduct()
                    .persistedAs("p1", Instant.parse("2025-01-01T00:00:00Z"),
                            Instant.parse("2025-01-01T00:00:00Z"))
                    .rated(5, 4)
                    .build();

            product.amend("New Title", List.of("A"), "d", Money.of(10.0), Money.of(12.0),
                    ProductCondition.GOOD, Quantity.of(3), List.of(category),
                    List.of(new Image("u", "a")));

            assertThat(product.getTitle()).isEqualTo("New Title");
            assertThat(product.getCondition()).isEqualTo(ProductCondition.GOOD);
            assertThat(product.getStock()).isEqualTo(Quantity.of(3));
            assertThat(product.getCategoryIds()).containsExactly(category);

            assertThat(product.getId()).isEqualTo("p1");
            assertThat(product.getAddedAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
            assertThat(product.getRatingCount()).isEqualTo(2);
            assertThat(product.getTotalRating()).isEqualTo(9);
            assertThat(product.getAverageRating()).isEqualTo(4.5);
        }

        @Test
        @DisplayName("the lists an aggregate hands out cannot be edited behind its back")
        void collectionsAreReadOnly() {
            Product product = ProductFixture.aProduct()
                    .withImages(new Image("u", "a"))
                    .by("A")
                    .build();

            assertThatThrownBy(() -> product.getImages().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> product.getAuthors().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("changePrice moves the price and nothing else")
        void changePriceMovesOnlyThePrice() {
            Product product = ProductFixture.aProduct().pricedAt(80.0).originallyPricedAt(90.0).build();

            product.changePrice(Money.of(70.0));

            assertThat(product.getPrice()).isEqualTo(Money.of(70.0));
            assertThat(product.getOriginalPrice()).isEqualTo(Money.of(90.0));
        }
    }

    @Test
    @DisplayName("isBelow is what the low-stock dashboard asks, with the threshold from config")
    void isBelowThreshold() {
        assertThat(ProductFixture.aProduct().withStock(9).build().isBelow(Quantity.of(10))).isTrue();
        assertThat(ProductFixture.aProduct().withStock(10).build().isBelow(Quantity.of(10))).isFalse();
    }
}
