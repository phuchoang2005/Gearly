package com.dominator.bookify.repository;

import com.dominator.bookify.model.State;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StateRepository extends MongoRepository<State, String> {
    List<State> findByCountryIdOrderByNameAsc(Integer countryId);
    Optional<State> findByNameIgnoreCaseAndCountryId(String name, Integer countryId);

}
