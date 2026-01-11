import { useQuery } from "@tanstack/react-query";
import {
    fetchCountries,
    fetchStates,
    fetchCities,
} from "@u_services/addressService.js";

export function useAddressData(selectedCountry, selectedState) {
    const countriesQuery = useQuery({
        queryKey: ["countries"],
        queryFn: fetchCountries,
        staleTime: 5 * 60_000,
    });

    const statesQuery = useQuery({
        queryKey: ["states", selectedCountry],
        queryFn: () => fetchStates(selectedCountry),
        enabled: Boolean(selectedCountry),
        staleTime: 5 * 60_000,
    });

    const citiesQuery = useQuery({
        queryKey: ["cities", selectedCountry, selectedState],
        queryFn: () => fetchCities({ countryId: selectedCountry, stateId: selectedState }),
        enabled: Boolean(selectedCountry && selectedState),
        staleTime: 5 * 60_000,
    });

    return {
        countries: countriesQuery.data ?? [],
        statesList: statesQuery.data ?? [],
        citiesList: citiesQuery.data ?? [],

        isLoadingCountries: countriesQuery.isLoading,
        isErrorCountries: countriesQuery.isError,

        isLoadingStates: statesQuery.isLoading,
        isErrorStates: statesQuery.isError,

        isLoadingCities: citiesQuery.isLoading,
        isErrorCities: citiesQuery.isError,

    };
}
