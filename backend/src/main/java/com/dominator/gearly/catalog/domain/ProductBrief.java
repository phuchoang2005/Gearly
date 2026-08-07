package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductId;

/**
 * Enough of a product to talk about one: what it is called, what it costs, how it is rated.
 *
 * <p>Distinct from {@link CatalogSnapshot}, which exists to be <em>copied onto</em> a cart or
 * order line at capture time and therefore carries stock and condition but no rating. This is
 * for a reader that means to describe a product rather than sell it — the assistant recommending
 * one — so it carries the rating and not the stock.
 *
 * <p>Two published values rather than one wider one, because the fields a snapshot must have are
 * fields an order line depends on: adding a rating to {@code CatalogSnapshot} would put it into
 * {@code OrderLine.fromSnapshot} and thence into every stored order line, for the benefit of a
 * caller that never places an order.
 */
public record ProductBrief(ProductId productId, String title, Money price, double averageRating) {
}
