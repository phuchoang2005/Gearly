package com.dominator.gearly.geo.api;

import com.dominator.gearly.geo.domain.Place;
import com.dominator.gearly.geo.domain.PlaceDirectory;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The cascading country/state/city dropdowns on the checkout and registration forms.
 *
 * <p>URLs, parameters and response shape unchanged — still {@code {value, label}} objects, which
 * is the shape the frontend's select components take.
 *
 * <p>What changed is what the controller receives. It used to get {@code Country}, {@code State}
 * and {@code City} entities — {@code @Document}s, straight out of a repository — and map them to
 * DTOs inline. The port hands it {@link Place}es, so the persistence shape stops at the adapter.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final PlaceDirectory places;

    @GetMapping("/countries")
    public List<AddressOptionDTO> fetchCountries() {
        return asOptions(places.countries());
    }

    @GetMapping("/states")
    public List<AddressOptionDTO> fetchStates(@RequestParam("countryId") Integer countryId) {
        return asOptions(places.statesOf(countryId));
    }

    @GetMapping("/cities")
    public List<AddressOptionDTO> fetchCities(
            @RequestParam("countryId") Integer countryId,
            @RequestParam("stateId") Integer stateId
    ) {
        return asOptions(places.citiesOf(countryId, stateId));
    }

    private static List<AddressOptionDTO> asOptions(List<Place> places) {
        return places.stream()
                .map(place -> new AddressOptionDTO(place.id(), place.name()))
                .toList();
    }
}
