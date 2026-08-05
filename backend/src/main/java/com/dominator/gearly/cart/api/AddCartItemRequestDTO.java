package com.dominator.gearly.cart.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * <b>🔒 The fix for the cart's price-tampering hole.</b> Everything a client is allowed to say
 * about adding something to a basket.
 *
 * <p>These three endpoints — {@code POST /api/cart/add}, {@code POST /api/cart/merge} and
 * {@code POST /api/guest-cart/add} — used to bind {@code @RequestBody CartItem}: the
 * persistence document itself. Its {@code price}, {@code title}, {@code stock} and
 * {@code condition} were therefore client-controlled, and the server persisted them without
 * ever re-reading the catalog. A customer could add a $1,599 graphics card to their cart at
 * $0.01 and check out with it. The S8 characterization suite pinned exactly that, labelled
 * {@code KNOWN BUG (S11 price tampering)}.
 *
 * <p>The DTO is not itself the fix — the fix is that {@code CartLine} can only be built from a
 * {@code CatalogSnapshot}, so there is nowhere for a submitted price to go. This is what makes
 * the contract say so.
 *
 * <h2>Frontend compatibility</h2>
 * The body shrank; it did not change shape. The storefront posts
 * {@code {productId, title, author, price, quantity, image, condition, stock}} and Spring
 * Boot ignores unknown properties by default, so the extra five are dropped on the floor and
 * no frontend change is required. Verified against
 * {@code frontend/src/components/user/products/ProductCard.jsx} and
 * {@code .../ProductDetailsPage/sections/ProductDetails.jsx}, which are the only two callers.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddCartItemRequestDTO {

    @NotBlank(message = "productId is required")
    private String productId;

    /** How many units to add. At least one — the aggregate would refuse zero anyway. */
    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity = 1;
}
