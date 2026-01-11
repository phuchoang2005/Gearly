package com.dominator.bookify.controller;

import com.dominator.bookify.dto.AddressOptionDTO;
import com.dominator.bookify.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService;

    @GetMapping("/countries")
    public List<AddressOptionDTO> fetchCountries() {
        return addressService.getAllCountries()
                .stream()
                .map(c -> new AddressOptionDTO(c.getId(), c.getName()))
                .collect(Collectors.toList());
    }

    @GetMapping("/states")
    public List<AddressOptionDTO> fetchStates(@RequestParam("countryId") Integer countryId) {
        return addressService.getStatesByCountryId(countryId)
                .stream()
                .map(s -> new AddressOptionDTO(s.getId(), s.getName()))
                .collect(Collectors.toList());
    }

    @GetMapping("/cities")
    public List<AddressOptionDTO> fetchCities(
            @RequestParam("countryId") Integer countryId,
            @RequestParam("stateId")   Integer stateId
    ) {
        return addressService.getCitiesByCountryIdAndStateId(countryId, stateId)
                .stream()
                .map(c -> new AddressOptionDTO(c.getId(), c.getName()))
                .collect(Collectors.toList());
    }
}
