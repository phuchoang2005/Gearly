package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.CartResponseDTO;
import com.dominator.gearly.model.Cart;
import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.model.CartItem;
import org.springframework.stereotype.Component;

/**
 * Builds {@link CartItem} lines from a {@link CatalogSnapshot} and maps {@link Cart}
 * entities to response DTOs. The cart stores a denormalized copy (title, price,
 * image, …) so it survives later catalog edits.
 */
@Component
public class CartMapper {

    /** Response view mirroring the entity's wire shape. */
    public CartResponseDTO toResponseDto(Cart cart) {
        CartResponseDTO dto = new CartResponseDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUserId());
        dto.setGuestId(cart.getGuestId());
        dto.setItems(cart.getItems());
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());
        return dto;
    }

    /**
     * New cart line for the given product with quantity 1.
     *
     * <p>Every field comes from the catalog's snapshot, which is what makes the line
     * trustworthy: the author fallback and the image guard that used to be written out here
     * are decisions {@code Product.snapshot()} has already made, in one place, for the cart
     * and the order line alike.
     */
    public CartItem toCartItem(CatalogSnapshot snapshot) {
        CartItem item = new CartItem();
        item.setProductId(snapshot.productId().value());
        item.setTitle(snapshot.title());
        item.setAuthor(snapshot.author());
        item.setPrice(snapshot.price());
        item.setQuantity(1);
        item.setImage(snapshot.imageUrl());
        item.setCondition(snapshot.condition());
        item.setStock(snapshot.stock().toInt());
        return item;
    }
}
