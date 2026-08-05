package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.DomainNotFoundException;
import com.dominator.gearly.shared.domain.ProductId;

/**
 * No product with that id.
 *
 * <p>Replaces {@code ProductService.getProductById} returning {@code null} on a miss — an
 * answer two of its own callers ({@code getStock}, {@code decreaseStock}) immediately
 * dereferenced, so a deleted product turned a checkout into a {@code NullPointerException}
 * and an opaque 500. Missing is now a stated outcome with a 404 behind it.
 */
public class ProductNotFoundException extends DomainNotFoundException {

    public ProductNotFoundException(ProductId productId) {
        super("Product not found: " + productId.value());
    }

    public ProductNotFoundException(String message) {
        super(message);
    }
}
