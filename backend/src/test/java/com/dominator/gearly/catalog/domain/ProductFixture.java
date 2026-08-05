package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.Rating;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Builds {@link Product} aggregates for tests. The catalog's counterpart to
 * {@code OrderFixture}, and it follows the same two rules that keep a fixture from quietly
 * becoming the setter surface the aggregate just got rid of:
 *
 * <ol>
 *   <li><b>State is reached through real behavior.</b> {@link Builder#rated} folds each star
 *       through {@code Product.addRating}, so a fixture can only describe a rollup the
 *       production code could have produced — a test cannot invent a product with 3 reviews
 *       totalling 900.</li>
 *   <li><b>Reflection touches only the persistence-managed fields</b> — {@code id},
 *       {@code version} and the two audit timestamps. Those are populated by Spring Data on
 *       load and by nothing else; a test that needs them is standing in for the mapper.</li>
 * </ol>
 */
public final class ProductFixture {

    private ProductFixture() {
    }

    public static Builder aProduct() {
        return new Builder();
    }

    /** The shape most tests want: one titled, priced, in-stock product with an image. */
    public static Product product(String id, double price, int stock) {
        return aProduct().withId(id)
                .titled("Product " + id)
                .by("Author " + id)
                .pricedAt(price)
                .withStock(stock)
                .withImages(new Image("http://img/" + id + ".png", "alt"))
                .build();
    }

    public static final class Builder {

        private String id;
        private Long version;
        private String title = "A product";
        private List<String> authors = List.of("An author");
        private String description;
        private Money price = Money.of(10.00);
        private Money originalPrice = Money.of(10.00);
        private ProductCondition condition = ProductCondition.NEW;
        private Quantity stock = Quantity.of(5);
        private List<CategoryId> categoryIds;
        private List<Image> images;
        private final List<Integer> ratings = new ArrayList<>();
        private Instant addedAt;
        private Instant modifiedAt;

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder titled(String title) {
            this.title = title;
            return this;
        }

        public Builder by(String... authors) {
            this.authors = List.of(authors);
            return this;
        }

        public Builder withoutAuthors() {
            this.authors = null;
            return this;
        }

        public Builder described(String description) {
            this.description = description;
            return this;
        }

        public Builder pricedAt(double price) {
            this.price = Money.of(price);
            return this;
        }

        public Builder originallyPricedAt(double originalPrice) {
            this.originalPrice = Money.of(originalPrice);
            return this;
        }

        public Builder inCondition(ProductCondition condition) {
            this.condition = condition;
            return this;
        }

        public Builder withStock(int stock) {
            this.stock = Quantity.of(stock);
            return this;
        }

        public Builder inCategories(CategoryId... categoryIds) {
            this.categoryIds = List.of(categoryIds);
            return this;
        }

        public Builder withImages(Image... images) {
            this.images = List.of(images);
            return this;
        }

        public Builder withoutImages() {
            this.images = null;
            return this;
        }

        /** Folds each star in through {@code addRating}, exactly as a real review would. */
        public Builder rated(int... stars) {
            Arrays.stream(stars).forEach(ratings::add);
            return this;
        }

        public Builder persistedAs(String id, Instant addedAt, Instant modifiedAt) {
            this.id = id;
            this.addedAt = addedAt;
            this.modifiedAt = modifiedAt;
            this.version = 0L;
            return this;
        }

        public Product build() {
            Product product = Product.create(title, authors, description, price, originalPrice,
                    condition, stock, categoryIds, images);

            ratings.forEach(stars -> product.addRating(Rating.of(stars)));

            setPersistenceField(product, "id", id);
            setPersistenceField(product, "version", version);
            setPersistenceField(product, "addedAt", addedAt);
            setPersistenceField(product, "modifiedAt", modifiedAt);
            return product;
        }

        private void setPersistenceField(Product product, String field, Object value) {
            if (value != null) {
                ReflectionTestUtils.setField(product, field, value);
            }
        }
    }
}
