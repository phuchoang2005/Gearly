package com.dominator.gearly.dto;

import com.dominator.gearly.model.CartItem;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * Response view of a {@link com.dominator.gearly.model.Cart}. Mirrors the entity
 * field-for-field so the wire shape is unchanged, keeping the persistence entity
 * out of the controller signature.
 */
@Getter
@Setter
public class CartResponseDTO {
    private String id;
    private String userId;
    private String guestId;
    private List<CartItem> items;
    private Date createdAt;
    private Date updatedAt;
}
