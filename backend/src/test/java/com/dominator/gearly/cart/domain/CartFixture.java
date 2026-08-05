package com.dominator.gearly.cart.domain;

import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

/**
 * Builds {@link Cart} aggregates for tests, following the same two rules as
 * {@code OrderFixture} and {@code ProductFixture}: state is reached through real behavior, and
 * reflection touches only the persistence-managed fields.
 *
 * <p>{@link Builder#holding} goes through {@code addLine}, so a fixture cannot describe a cart
 * line the production code could not have created — in particular it cannot describe one at a
 * price the catalog never published, which is exactly the state the old fixtures had to be
 * able to describe in order to pin the price-tampering bug.
 */
public final class CartFixture {

    private CartFixture() {
    }

    /** What the catalog publishes about a product, in the shape the cart consumes it. */
    public static CatalogSnapshot snapshot(String id, double price, int stock) {
        return new CatalogSnapshot(ProductId.of(id), "Product " + id, "Author " + id,
                Money.of(price), "http://img/" + id + ".png", ProductCondition.NEW,
                Quantity.of(stock));
    }

    public static Builder aCart() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private UserId userId;
        private String guestId;
        private Long version;
        private Instant createdAt;
        private Instant updatedAt;
        private final java.util.List<Runnable> operations = new java.util.ArrayList<>();
        private Cart cart;

        public Builder ownedBy(String userId) {
            this.userId = UserId.of(userId);
            this.guestId = null;
            return this;
        }

        public Builder forGuest(String guestId) {
            this.guestId = guestId;
            this.userId = null;
            return this;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        /** Adds a line the way a customer would — through {@code addLine}, from a snapshot. */
        public Builder holding(CatalogSnapshot snapshot, int quantity) {
            operations.add(() -> cart.addLine(snapshot, Quantity.of(quantity)));
            return this;
        }

        public Builder holding(String productId, double price, int stock, int quantity) {
            return holding(snapshot(productId, price, stock), quantity);
        }

        public Builder persistedAs(String id, Instant createdAt, Instant updatedAt) {
            this.id = id;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
            this.version = 0L;
            return this;
        }

        public Cart build() {
            cart = userId != null ? Cart.forUser(userId) : Cart.forGuest(guestId);
            operations.forEach(Runnable::run);

            setPersistenceField("id", id);
            setPersistenceField("version", version);
            setPersistenceField("createdAt", createdAt);
            setPersistenceField("updatedAt", updatedAt);

            // A fixture stands in for a cart that has been loaded, not one that has just been
            // changed — so the in-memory "needs writing" flag the builder's own addLine calls
            // set is cleared. Without this every fixture would look dirty and the "an
            // unchanged cart is not re-saved" assertion could never be true.
            ReflectionTestUtils.setField(cart, "dirty", false);
            return cart;
        }

        private void setPersistenceField(String field, Object value) {
            if (value != null) {
                ReflectionTestUtils.setField(cart, field, value);
            }
        }
    }
}
