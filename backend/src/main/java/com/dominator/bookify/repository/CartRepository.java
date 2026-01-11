package com.dominator.bookify.repository;

import com.dominator.bookify.model.Cart;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByUserId(String userId);
    Optional<Cart> findByGuestId(String guestId);
    void deleteByGuestId(String guestId);
}