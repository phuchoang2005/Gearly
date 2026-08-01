package com.dominator.gearly.mapper;

import com.dominator.gearly.model.Book;
import com.dominator.gearly.model.OrderItem;
import org.springframework.stereotype.Component;

/**
 * Builds {@link OrderItem} snapshots from a {@link Book} at order-placement time,
 * capturing the price and title as they were when the order was made.
 */
@Component
public class OrderMapper {

    public OrderItem toOrderItem(Book book, int quantity) {
        OrderItem item = new OrderItem();
        item.setBookId(book.getId());
        item.setImageUrl(book.getImages().getFirst().getUrl());
        item.setTitle(book.getTitle());
        item.setPrice(book.getPrice());
        item.setQuantity(quantity);
        return item;
    }
}
