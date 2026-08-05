package com.dominator.gearly.cart.infrastructure;

import com.dominator.gearly.cart.domain.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/** Spring Data's half of the cart adapter. Package-private — see {@code MongoCartRepository}. */
interface SpringDataCartRepository extends MongoRepository<Cart, String> {

    Optional<Cart> findByUserId(String userId);

    Optional<Cart> findByGuestId(String guestId);

    void deleteByGuestId(String guestId);
}
