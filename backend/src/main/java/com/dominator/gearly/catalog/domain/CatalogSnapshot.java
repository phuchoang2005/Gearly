package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;

/**
 * <b>The anti-corruption layer between Catalog and everything downstream of it.</b> What a
 * product looked like at one moment, as a value.
 *
 * <p>Cart and Ordering are customers of Catalog, and until now they were customers with a key
 * to the house: {@code CartService} took a {@code Product} and read seven fields off it,
 * {@code OrderMapper.toOrderItem} took another and read five. Every one of those reads was a
 * commitment that {@code Product} would keep that shape. This record is the commitment
 * instead — a downstream context sees these seven fields and nothing else, so the aggregate
 * behind it is free to change.
 *
 * <h2>Snapshot, not reference — and deliberately so</h2>
 * A cart line and an order line each keep their own copy of the title, price and image, taken
 * when the line was created. That is not laziness about staleness; it is the rule. A price
 * change must not silently rewrite what a customer already has in their basket, and it
 * certainly must not rewrite what they have already paid. The working agreement states it as
 * "reference other aggregates by typed id only" — {@link #productId()} is the only durable
 * link, and everything beside it is a copy with a timestamp implied.
 *
 * <p>{@code stock} is the exception that proves the rule: it is the one field a downstream
 * context must <em>not</em> trust for long, which is why {@code Cart.reconcileWith} re-reads
 * it on every load rather than believing the copy on the line.
 *
 * <p>ArchUnit lets other contexts name this type because it ends in {@code Snapshot} and lives
 * in a {@code domain} package — see {@code contexts_touch_each_other_only_through_published_types}.
 */
public record CatalogSnapshot(ProductId productId,
                              String title,
                              String author,
                              Money price,
                              String imageUrl,
                              ProductCondition condition,
                              Quantity stock) {

    /**
     * The same check {@link Product#reserve} makes, asked of a copy rather than of the
     * aggregate — because the cart needs to know whether a quantity <em>would</em> fit
     * without taking the units.
     *
     * <p>It delegates rather than restating, so there is still exactly one comparison of a
     * wanted quantity against an available one in the codebase.
     */
    public void assertCanSupply(Quantity wanted) {
        InsufficientStockException.requireAtLeast(stock, wanted, title);
    }

    /** How many units of this can actually be supplied — the caller's ask, clamped. */
    public Quantity clamp(Quantity wanted) {
        return wanted.isLessThan(stock) ? wanted : stock;
    }

    public boolean isOutOfStock() {
        return stock.isZero();
    }
}
