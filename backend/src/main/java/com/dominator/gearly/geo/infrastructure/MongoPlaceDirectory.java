package com.dominator.gearly.geo.infrastructure;

import com.dominator.gearly.geo.domain.City;
import com.dominator.gearly.geo.domain.Country;
import com.dominator.gearly.geo.domain.Place;
import com.dominator.gearly.geo.domain.PlaceDirectory;
import com.dominator.gearly.geo.domain.ResolvedPlace;
import com.dominator.gearly.geo.domain.State;
import com.dominator.gearly.geo.domain.UnknownPlaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The {@link PlaceDirectory} adapter over the three seeded geo collections.
 *
 * <p>Was {@code service.common.AddressService}, which every caller reached directly and which
 * returned raw {@code @Document} entities to a controller that mapped them inline.
 */
@Repository
@RequiredArgsConstructor
public class MongoPlaceDirectory implements PlaceDirectory {

    private final SpringDataCountryRepository countries;
    private final SpringDataStateRepository states;
    private final SpringDataCityRepository cities;

    @Override
    public List<Place> countries() {
        return countries.findAllByOrderByNameAsc().stream()
                .map(country -> new Place(country.getId(), country.getName()))
                .toList();
    }

    @Override
    public List<Place> statesOf(int countryId) {
        return states.findByCountryIdOrderByNameAsc(countryId).stream()
                .map(state -> new Place(state.getId(), state.getName()))
                .toList();
    }

    @Override
    public List<Place> citiesOf(int countryId, int stateId) {
        return cities.findByCountryIdAndStateIdOrderByNameAsc(countryId, stateId).stream()
                .map(city -> new Place(city.getId(), city.getName()))
                .toList();
    }

    /**
     * Resolves as far down the hierarchy as the caller gave names for.
     *
     * <p>The cascade is deliberate: no country means no state to look up, because
     * {@code findByNameIgnoreCaseAndCountryId} takes a country id and passing zero would search
     * for a state in a country that does not exist. The old code did the same threading, spread
     * across three call sites in {@code AuthService.resolveAddress}, where nothing enforced it.
     */
    @Override
    public ResolvedPlace resolve(String countryName, String stateName, String cityName) {
        if (isBlank(countryName)) {
            return ResolvedPlace.NONE;
        }
        int countryId = required("country", countryName,
                countries.findByNameIgnoreCase(countryName.trim()).map(Country::getId));

        if (isBlank(stateName)) {
            return new ResolvedPlace(countryId, 0, 0);
        }
        int stateId = required("state", stateName,
                states.findByNameIgnoreCaseAndCountryId(stateName.trim(), countryId).map(State::getId));

        if (isBlank(cityName)) {
            return new ResolvedPlace(countryId, stateId, 0);
        }
        int cityId = required("city", cityName,
                cities.findByNameIgnoreCaseAndStateIdAndCountryId(cityName.trim(), stateId, countryId)
                        .map(City::getId));

        return new ResolvedPlace(countryId, stateId, cityId);
    }

    /**
     * The id, or a 400 naming what could not be found.
     *
     * <p>This is the line that used to be {@code .orElse(null)} — unboxed into an {@code int} by
     * the caller, so a miss was a {@link NullPointerException} and a 500.
     */
    private static int required(String kind, String name, Optional<Integer> found) {
        return found.orElseThrow(() -> new UnknownPlaceException(kind, name));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
