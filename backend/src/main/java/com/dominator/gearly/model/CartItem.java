package com.dominator.gearly.model;

import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A line in a cart, holding a denormalized copy of the product's title, price, image and
 * condition so the cart survives a later catalog edit.
 *
 * <p>The {@code @Document} annotation is a copy-paste artifact — cart items are only ever
 * embedded in a cart. S11 turns this into {@code CartLine} in {@code cart.domain},
 * hydrated from a {@code CatalogSnapshot} rather than bound from the request body.
 */
@Document(collection = "cartItem")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    private String productId;
    private String title;
    private String author;
    private Money price = Money.ZERO;
    private int quantity;
    private String image;
    private ProductCondition condition;
    private int stock;
}
