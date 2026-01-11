package com.dominator.bookify.service;

import com.dominator.bookify.model.Country;
import com.dominator.bookify.model.State;
import com.dominator.bookify.model.City;
import com.dominator.bookify.repository.CountryRepository;
import com.dominator.bookify.repository.StateRepository;
import com.dominator.bookify.repository.CityRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@AllArgsConstructor
@Service
public class AddressService {
    private final CountryRepository countryRepo;
    private final StateRepository stateRepo;
    private final CityRepository cityRepo;

    public List<Country> getAllCountries() {
        return countryRepo.findAllByOrderByNameAsc();
    }

    public List<State> getStatesByCountryId(Integer countryId) {
        return stateRepo.findByCountryIdOrderByNameAsc(countryId);
    }

    public List<City> getCitiesByCountryIdAndStateId(Integer countryId, Integer stateId) {
        return cityRepo.findByCountryIdAndStateIdOrderByNameAsc(countryId, stateId);
    }

    public Integer getCountryIdByName(String countryName) {
        return countryRepo.findByNameIgnoreCase(countryName)
                .map(Country::getId)
                .orElse(null);
    }

    public Integer getStateIdByName(String stateName, Integer countryId) {
        return stateRepo.findByNameIgnoreCaseAndCountryId(stateName, countryId)
                .map(State::getId)
                .orElse(null);
    }

    public Integer getCityIdByName(String cityName, Integer stateId, Integer countryId) {
        return cityRepo.findByNameIgnoreCaseAndStateIdAndCountryId(cityName, stateId, countryId)
                .map(City::getId)
                .orElse(null);
    }
}
