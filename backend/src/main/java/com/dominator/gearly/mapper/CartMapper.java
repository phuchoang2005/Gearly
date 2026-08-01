package com.dominator.gearly.mapper;

import com.dominator.gearly.model.Book;
import com.dominator.gearly.model.CartItem;
import org.springframework.stereotype.Component;

/**
 * Builds {@link CartItem} snapshots from a {@link Book}. The cart stores a
 * denormalized copy (title, price, image, …) so it survives later catalog edits.
 */
@Component
public class CartMapper {

    /** New cart line for the given book with quantity 1. */
    public CartItem toCartItem(Book book) {
        CartItem item = new CartItem();
        item.setBookId(book.getId());
        item.setTitle(book.getTitle());
        item.setAuthor(book.getAuthors() != null && !book.getAuthors().isEmpty()
                ? book.getAuthors().getFirst()
                : "Unknown");
        item.setPrice(book.getPrice());
        item.setQuantity(1);
        item.setImage((book.getImages() != null && !book.getImages().isEmpty())
                ? book.getImages().get(0).getUrl()
                : null);
        item.setCondition(book.getCondition());
        item.setStock(book.getStock());
        return item;
    }
}
