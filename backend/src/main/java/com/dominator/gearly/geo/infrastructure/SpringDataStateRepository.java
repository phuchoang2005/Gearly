package com.dominator.gearly.geo.infrastructure;

import com.dominator.gearly.geo.domain.State;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

/** Spring Data's view of the states collection. Reached only through {@link MongoPlaceDirectory}. */
interface SpringDataStateRepository extends MongoRepository<State, String> {
    List<State> findByCountryIdOrderByNameAsc(Integer countryId);
    Optional<State> findByNameIgnoreCaseAndCountryId(String name, Integer countryId);

}
