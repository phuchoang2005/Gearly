package com.dominator.bookify.repository;

import com.dominator.bookify.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    List<User> findAllByFullNameContainingIgnoreCase(String fullName);

    List<User> findAllByEmailContainingIgnoreCase(String email);

    List<User> findAllByFullNameContainingIgnoreCaseAndEmailContainingIgnoreCase(
            String fullName,
            String email
    );
}
