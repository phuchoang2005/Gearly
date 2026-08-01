package com.dominator.gearly.mapper;

import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.CartItem;
import org.springframework.stereotype.Component;

/**
 * Builds {@link CartItem} snapshots from a {@link Product}. The cart stores a
 * denormalized copy (title, price, image, …) so it survives later catalog edits.
 */
@Component
public class CartMapper {

    /** New cart line for the given product with quantity 1. */
    public CartItem toCartItem(Product product) {
        CartItem item = new CartItem();
        item.setProductId(product.getId());
        item.setTitle(product.getTitle());
        item.setAuthor(product.getAuthors() != null && !product.getAuthors().isEmpty()
                ? product.getAuthors().getFirst()
                : "Unknown");
        item.setPrice(product.getPrice());
        item.setQuantity(1);
        item.setImage((product.getImages() != null && !product.getImages().isEmpty())
                ? product.getImages().get(0).getUrl()
                : null);
        item.setCondition(product.getCondition());
        item.setStock(product.getStock());
        return item;
    }
}
