package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.data.annotation.PersistenceCreator;

/**
 * One line of an order: what was bought, how many, and the product's title, price and image
 * <em>as they were at the moment the order was placed</em>.
 *
 * <p>The snapshot is the point. A later catalog edit — a price change, a retitle, a delisting
 * — must not rewrite what a customer already agreed to pay, so an order line copies those
 * fields and never holds a {@code Product}. It references the catalog by {@link ProductId}
 * alone, which is also the only thing S11's {@code CatalogSnapshot} anti-corruption layer
 * will keep once it takes over building these.
 *
 * <p>Was {@code model.OrderItem}, with a {@code @Document(collection = "orderItem")} that
 * named a collection which does not exist. Immutable, and typed: {@code productId} is a
 * {@code ProductId} rather than a {@code String} the compiler would happily accept a user id
 * for, and {@code quantity} is a {@code Quantity}, which cannot be negative. Both store and
 * serialize exactly as they did — a bare string and a bare int.
 */
@Getter
public class OrderLine {

    private final ProductId productId;
    private final String title;
    private final Money price;
    private final String imageUrl;
    private final Quantity quantity;

    @PersistenceCreator
    @JsonCreator
    public OrderLine(@JsonProperty("productId") ProductId productId,
                     @JsonProperty("title") String title,
                     @JsonProperty("price") Money price,
                     @JsonProperty("imageUrl") String imageUrl,
                     @JsonProperty("quantity") Quantity quantity) {
        this.productId = productId;
        this.title = title;
        // a document written before S9 may have no price field; it read as zero then, and now
        this.price = price == null ? Money.ZERO : price;
        this.imageUrl = imageUrl;
        this.quantity = quantity == null ? Quantity.ZERO : quantity;
    }

    /**
     * A line for {@code quantity} units of what the catalog is publishing right now.
     *
     * <p>This replaces {@code OrderMapper.toOrderItem}, and with it the thing that class did
     * that this cannot: reach into a {@code Product} and call {@code getImages().getFirst()}
     * unguarded, which threw {@code NoSuchElementException} on any product without a picture
     * and turned checkout into a 500. A snapshot has already decided what its image is —
     * possibly {@code null} — so there is nothing left here to get wrong.
     *
     * <p>The check comes first: an order line for more units than exist must not be
     * constructible at all.
     *
     * @throws com.dominator.gearly.catalog.domain.InsufficientStockException if the catalog
     *         cannot supply that many
     */
    public static OrderLine fromSnapshot(CatalogSnapshot snapshot, Quantity quantity) {
        snapshot.assertCanSupply(quantity);
        return new OrderLine(
                snapshot.productId(),
                snapshot.title(),
                snapshot.price(),
                snapshot.imageUrl(),
                quantity);
    }

    /** What this line costs before tax and shipping. */
    public Money lineTotal() {
        return price.times(quantity);
    }
}
