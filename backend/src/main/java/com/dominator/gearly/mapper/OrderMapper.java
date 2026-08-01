package com.dominator.gearly.mapper;

import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.OrderItem;
import org.springframework.stereotype.Component;

/**
 * Builds {@link OrderItem} snapshots from a {@link Product} at order-placement time,
 * capturing the price and title as they were when the order was made.
 */
@Component
public class OrderMapper {

    public OrderItem toOrderItem(Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setImageUrl(product.getImages().getFirst().getUrl());
        item.setTitle(product.getTitle());
        item.setPrice(product.getPrice());
        item.setQuantity(quantity);
        return item;
    }
}
