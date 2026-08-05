package com.dominator.gearly.cart.domain;

import com.dominator.gearly.shared.domain.UserId;

import java.util.Optional;

/** The cart port. {@code MongoCartRepository} is the adapter behind it. */
public interface CartRepository {

    Optional<Cart> findByUser(UserId userId);

    Optional<Cart> findByGuest(String guestId);

    Cart save(Cart cart);

    void deleteByGuest(String guestId);
}
