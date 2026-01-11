package com.dominator.bookify.repository;

import com.dominator.bookify.model.City;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CityRepository extends MongoRepository<City, String> {
    List<City> findByCountryIdAndStateIdOrderByNameAsc(Integer countryId, Integer stateId);
    Optional<City> findByNameIgnoreCaseAndStateIdAndCountryId(String name, Integer stateId, Integer countryId);
}
