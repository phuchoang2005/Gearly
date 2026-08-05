package com.dominator.gearly.cart.infrastructure;

import com.dominator.gearly.cart.domain.Cart;
import com.dominator.gearly.cart.domain.CartRepository;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * The MongoDB adapter behind {@link CartRepository}. The only class in the cart context that
 * knows the owner is stored as a plain string on the document; the typed id is unwrapped here
 * and nowhere else.
 */
@Repository
@RequiredArgsConstructor
public class MongoCartRepository implements CartRepository {

    private final SpringDataCartRepository carts;

    @Override
    public Optional<Cart> findByUser(UserId userId) {
        return carts.findByUserId(userId.value());
    }

    @Override
    public Optional<Cart> findByGuest(String guestId) {
        return carts.findByGuestId(guestId);
    }

    @Override
    public Cart save(Cart cart) {
        return carts.save(cart);
    }

    @Override
    public void deleteByGuest(String guestId) {
        carts.deleteByGuestId(guestId);
    }
}
