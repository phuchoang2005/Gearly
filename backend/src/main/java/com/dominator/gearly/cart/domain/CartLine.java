package com.dominator.gearly.cart.domain;

import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.data.annotation.PersistenceCreator;

/**
 * One line of a cart: what the customer means to buy, how many, and the product's title,
 * price, image and condition as the catalog had them when the line was created.
 *
 * <p>Was {@code model.CartItem}, a Lombok {@code @Data} bag carrying a
 * {@code @Document(collection = "cartItem")} that named a collection which does not exist —
 * the sixth stray {@code @Document} of the refactor. Cart lines are only ever embedded in a
 * cart.
 *
 * <h2>Why this type used to be a security hole</h2>
 * {@code CartController} bound it straight off the request body. A customer could post any
 * {@code price}, {@code title}, {@code stock} or {@code condition} they liked and the server
 * persisted it without ever re-reading the catalog — the S8 suite pinned a line stored at
 * $0.01. That is impossible now, and not by validation: the only way to make a line is
 * {@link #fromSnapshot}, and the only thing a request contributes is a product id and a
 * quantity.
 *
 * <h2>What is refreshed, and what is deliberately not</h2>
 * {@code stock} is a display hint — the storefront greys out the quantity stepper with it —
 * and it is the one copied field that must <em>not</em> be trusted to stay true. It is
 * refreshed on every load by {@link Cart#reconcileWith}. The rest are snapshots and stay put:
 * "reference other aggregates by typed id only, and copy at capture time" is the working
 * agreement, and a price that rewrites itself under a customer is the thing it exists to
 * prevent.
 *
 * <p>(That does leave a cart able to display a price the order will not charge, because
 * placement re-reads the catalog. It is pre-existing, it is not what this sprint is about, and
 * changing it is a product decision rather than a refactor.)
 *
 * <p>Stores and serializes exactly as {@code CartItem} did: {@code productId} a bare string,
 * {@code quantity} and {@code stock} bare ints.
 */
@Getter
public class CartLine {

    private final ProductId productId;
    private final String title;
    private final String author;
    private final Money price;
    private Quantity quantity;
    private final String image;
    private final ProductCondition condition;
    private Quantity stock;

    @PersistenceCreator
    @JsonCreator
    CartLine(@JsonProperty("productId") ProductId productId,
             @JsonProperty("title") String title,
             @JsonProperty("author") String author,
             @JsonProperty("price") Money price,
             @JsonProperty("quantity") Quantity quantity,
             @JsonProperty("image") String image,
             @JsonProperty("condition") ProductCondition condition,
             @JsonProperty("stock") Quantity stock) {
        this.productId = productId;
        this.title = title;
        this.author = author;
        // a document written before S9 may have no price field; it read as zero then, and now
        this.price = price == null ? Money.ZERO : price;
        this.quantity = quantity == null ? Quantity.ZERO : quantity;
        this.image = image;
        this.condition = condition;
        this.stock = stock == null ? Quantity.ZERO : stock;
    }

    /**
     * A line for {@code quantity} units of what the catalog is publishing right now. The only
     * way to create one.
     *
     * <p>The check comes first, so a line for more units than exist is not constructible.
     *
     * @throws com.dominator.gearly.catalog.domain.InsufficientStockException if the catalog
     *         cannot supply that many
     */
    static CartLine fromSnapshot(CatalogSnapshot snapshot, Quantity quantity) {
        snapshot.assertCanSupply(quantity);
        return new CartLine(
                snapshot.productId(),
                snapshot.title(),
                snapshot.author(),
                snapshot.price(),
                quantity,
                snapshot.imageUrl(),
                snapshot.condition(),
                snapshot.stock());
    }

    /** What this line costs, for a caller that wants a basket subtotal. */
    public Money lineTotal() {
        return price.times(quantity);
    }

    boolean isFor(ProductId candidate) {
        return productId.equals(candidate);
    }

    void changeQuantityTo(Quantity newQuantity, CatalogSnapshot snapshot) {
        snapshot.assertCanSupply(newQuantity);
        this.stock = snapshot.stock();
        this.quantity = newQuantity;
    }

    void increaseBy(Quantity extra, CatalogSnapshot snapshot) {
        changeQuantityTo(quantity.plus(extra), snapshot);
    }

    /** Removes {@code units}, floored at zero — a line cannot go negative. */
    void decreaseBy(Quantity units) {
        this.quantity = units.isAtLeast(quantity) ? Quantity.ZERO : quantity.minus(units);
    }

    /**
     * Brings the line back in line with the catalog: refresh the stock hint, clamp the
     * quantity to what is actually available.
     *
     * @return whether anything changed, so the cart knows whether it needs saving
     */
    boolean reconcileWith(CatalogSnapshot snapshot) {
        Quantity clamped = snapshot.clamp(quantity);
        boolean changed = !snapshot.stock().equals(stock) || !clamped.equals(quantity);

        this.stock = snapshot.stock();
        this.quantity = clamped;
        return changed;
    }

    boolean isEmpty() {
        return quantity.isZero();
    }
}
