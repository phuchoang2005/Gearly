package com.dominator.gearly.geo.domain;

import java.util.List;

/**
 * The reference dataset of countries, states and cities.
 *
 * <p>Two jobs, and they are the two things anyone asks a geo dataset. Filling the storefront's
 * cascading dropdowns is {@link #countries()} / {@link #statesOf} / {@link #citiesOf}. Turning
 * the names those dropdowns submit back into the ids an {@code Address} stores is
 * {@link #resolve}.
 *
 * <p>A port rather than a service class because Identity depends on it: registration resolves an
 * address, and {@code contexts_touch_each_other_only_through_published_types} is satisfied by an
 * interface in a {@code domain} package and by nothing else. Before S13 that dependency was on
 * {@code service.common.AddressService} — a concrete class in a legacy package, holding three
 * Spring Data repositories.
 */
public interface PlaceDirectory {

    List<Place> countries();

    List<Place> statesOf(int countryId);

    List<Place> citiesOf(int countryId, int stateId);

    /**
     * Turns a country/state/city triple of names into the ids an address stores.
     *
     * <p>One call rather than three, because the lookups are not independent: a state is only
     * meaningful within its country and a city within its state, so doing them separately means
     * every caller has to remember the order and thread the intermediate ids through. The old
     * three-method version had exactly one caller and it got that threading right — but it also
     * had to, and nothing would have told it otherwise.
     *
     * <p>Blank or absent names resolve to {@link ResolvedPlace#NONE}; the address is optional at
     * registration. Anything below an unresolved level is not looked up, since a city id is
     * meaningless without the state it belongs to.
     *
     * @throws UnknownPlaceException if a name was supplied but is not in the dataset
     */
    ResolvedPlace resolve(String countryName, String stateName, String cityName);
}
