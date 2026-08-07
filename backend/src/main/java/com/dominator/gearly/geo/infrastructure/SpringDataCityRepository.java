package com.dominator.gearly.geo.infrastructure;

import com.dominator.gearly.geo.domain.City;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.Optional;

/** Spring Data's view of the cities collection. Reached only through {@link MongoPlaceDirectory}. */
interface SpringDataCityRepository extends MongoRepository<City, String> {
    List<City> findByCountryIdAndStateIdOrderByNameAsc(Integer countryId, Integer stateId);
    Optional<City> findByNameIgnoreCaseAndStateIdAndCountryId(String name, Integer stateId, Integer countryId);
}
