package com.dominator.gearly.model;

import com.dominator.gearly.shared.domain.Money;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A line on an order: the product's title and price <em>as they were</em> when the order
 * was placed, so a later catalog edit cannot rewrite order history.
 *
 * <p>The {@code @Document} annotation is a copy-paste artifact — order items are only ever
 * embedded in an order, never a standalone collection. S10 drops it when this type becomes
 * {@code OrderLine} in {@code ordering.domain}.
 */
@Document(collection = "orderItem")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderItem {
    private String productId;
    private String title;
    private Money price = Money.ZERO;
    private String imageUrl;
    private int quantity;
}
