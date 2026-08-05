package com.dominator.gearly.cart.api;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One line of a guest basket being folded into a user's at login.
 *
 * <p>Same story as {@link AddCartItemRequestDTO}: the merge endpoint bound
 * {@code List<CartItem>}, so it was the third way to post a price the server would believe.
 * The storefront sends the guest cart straight back as it received it —
 * {@code AuthContext.jsx} reads {@code res.data.items} and posts the array unchanged — so the
 * extra fields arrive and are ignored, and no frontend change is required.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MergeCartLineDTO {

    @NotBlank(message = "productId is required")
    private String productId;

    @Min(value = 1, message = "quantity must be at least 1")
    private int quantity = 1;
}
