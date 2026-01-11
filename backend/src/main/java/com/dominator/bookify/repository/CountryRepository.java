package com.dominator.bookify.repository;

import com.dominator.bookify.model.Country;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CountryRepository extends MongoRepository<Country, String> {
    List<Country> findAllByOrderByNameAsc();
    Optional<Country> findByNameIgnoreCase(String name);

}
