package com.dominator.gearly.cart.api;

import com.dominator.gearly.cart.domain.Cart;
import org.springframework.stereotype.Component;

/**
 * Maps the {@link Cart} aggregate to its response shape.
 *
 * <p>Was half of {@code mapper.CartMapper}. The other half — {@code toCartItem(Product)},
 * which built a line by reading seven fields off a catalog aggregate — is gone: a line is
 * built by {@code CartLine.fromSnapshot} inside the domain, which is what stops a request
 * body from ever contributing one.
 */
@Component
public class CartResponseMapper {

    public CartResponseDTO toResponseDto(Cart cart) {
        CartResponseDTO dto = new CartResponseDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUserId() == null ? null : cart.getUserId().value());
        dto.setGuestId(cart.getGuestId());
        dto.setItems(cart.getItems());
        dto.setCreatedAt(cart.getCreatedAt());
        dto.setUpdatedAt(cart.getUpdatedAt());
        return dto;
    }
}
