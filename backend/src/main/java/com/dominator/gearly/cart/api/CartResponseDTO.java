package com.dominator.gearly.cart.api;

import com.dominator.gearly.cart.domain.CartLine;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Response view of a {@link com.dominator.gearly.cart.domain.Cart}. Mirrors the aggregate
 * field-for-field so the wire shape is unchanged, keeping the persistence entity out of the
 * controller signature.
 *
 * <p>{@code userId} is a bare string here where the aggregate holds a {@code UserId}, which is
 * what it serialized as anyway — the typed id is unwrapped by {@code CartResponseMapper} so
 * the mapping is stated rather than relying on a Jackson annotation two packages away.
 */
@Getter
@Setter
public class CartResponseDTO {
    private String id;
    private String userId;
    private String guestId;
    private List<CartLine> items;
    private Instant createdAt;
    private Instant updatedAt;
}
