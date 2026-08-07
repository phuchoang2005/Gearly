package com.dominator.gearly.geo.infrastructure;

import com.dominator.gearly.geo.domain.Country;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

/** Spring Data's view of the countries collection. Reached only through {@link MongoPlaceDirectory}. */
interface SpringDataCountryRepository extends MongoRepository<Country, String> {
    List<Country> findAllByOrderByNameAsc();
    Optional<Country> findByNameIgnoreCase(String name);

}
