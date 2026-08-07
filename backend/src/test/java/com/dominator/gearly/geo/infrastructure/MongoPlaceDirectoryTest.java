package com.dominator.gearly.geo.infrastructure;

import com.dominator.gearly.geo.domain.City;
import com.dominator.gearly.geo.domain.Country;
import com.dominator.gearly.geo.domain.Place;
import com.dominator.gearly.geo.domain.ResolvedPlace;
import com.dominator.gearly.geo.domain.State;
import com.dominator.gearly.geo.domain.UnknownPlaceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The geo lookups, and in particular the one that used to be a 500.
 *
 * <p>{@code AddressService} returned {@code .orElse(null)} from all three lookups and
 * {@code AuthService.resolveAddress} assigned each to an {@code int}. Unboxing null throws, so
 * every one of the cases below — an unknown country, an absent country, a state that is not in
 * the country given — crashed a registration with "Internal server error". None of them had a
 * test, because neither class was reachable without the whole Spring context.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MongoPlaceDirectoryTest {

    @Mock private SpringDataCountryRepository countries;
    @Mock private SpringDataStateRepository states;
    @Mock private SpringDataCityRepository cities;

    private MongoPlaceDirectory directory;

    @BeforeEach
    void setUp() {
        directory = new MongoPlaceDirectory(countries, states, cities);
    }

    /** The entities have no public constructor by design, so the fields go in reflectively. */
    private static <T> T entity(Class<T> type, int id, String name) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            T instance = constructor.newInstance();
            ReflectionTestUtils.setField(instance, "id", id);
            ReflectionTestUtils.setField(instance, "name", name);
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void datasetHasVietnamHanoi() {
        when(countries.findByNameIgnoreCase("Vietnam"))
                .thenReturn(Optional.of(entity(Country.class, 704, "Vietnam")));
        when(states.findByNameIgnoreCaseAndCountryId("Hanoi", 704))
                .thenReturn(Optional.of(entity(State.class, 3757, "Hanoi")));
        when(cities.findByNameIgnoreCaseAndStateIdAndCountryId("Ba Dinh", 3757, 704))
                .thenReturn(Optional.of(entity(City.class, 21578, "Ba Dinh")));
    }

    // ---- resolving ---------------------------------------------------------

    @Nested
    @DisplayName("resolve")
    class Resolve {

        @Test
        @DisplayName("a full, known address resolves to its three ids")
        void resolvesAll() {
            datasetHasVietnamHanoi();

            assertThat(directory.resolve("Vietnam", "Hanoi", "Ba Dinh"))
                    .isEqualTo(new ResolvedPlace(704, 3757, 21578));
        }

        /**
         * The registration DTO does not mark country, state or city {@code @NotBlank}, so this
         * is a request the API accepts — and before S13 it was a guaranteed 500.
         */
        @ParameterizedTest(name = "country=[{0}]")
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("no country at all resolves to nothing, rather than throwing")
        void absentCountryIsNone(String country) {
            assertThat(directory.resolve(country, "Hanoi", "Ba Dinh")).isEqualTo(ResolvedPlace.NONE);
        }

        @Test
        @DisplayName("a country with no state resolves only the country")
        void partialAddress() {
            datasetHasVietnamHanoi();

            assertThat(directory.resolve("Vietnam", null, null))
                    .isEqualTo(new ResolvedPlace(704, 0, 0));
        }

        @Test
        @DisplayName("a country and state with no city leaves the city unset")
        void countryAndStateOnly() {
            datasetHasVietnamHanoi();

            assertThat(directory.resolve("Vietnam", "Hanoi", ""))
                    .isEqualTo(new ResolvedPlace(704, 3757, 0));
        }

        @Test
        @DisplayName("an unrecognised country is a refusal that names it, not a crash")
        void unknownCountry() {
            when(countries.findByNameIgnoreCase("Atlantis")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> directory.resolve("Atlantis", "Hanoi", "Ba Dinh"))
                    .isInstanceOf(UnknownPlaceException.class)
                    .hasMessageContaining("country")
                    .hasMessageContaining("Atlantis");
        }

        @Test
        @DisplayName("a state that is not in the given country is refused")
        void unknownState() {
            datasetHasVietnamHanoi();
            when(states.findByNameIgnoreCaseAndCountryId("Bavaria", 704)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> directory.resolve("Vietnam", "Bavaria", "Ba Dinh"))
                    .isInstanceOf(UnknownPlaceException.class)
                    .hasMessageContaining("state");
        }

        @Test
        @DisplayName("a city that is not in the given state is refused")
        void unknownCity() {
            datasetHasVietnamHanoi();
            when(cities.findByNameIgnoreCaseAndStateIdAndCountryId("Springfield", 3757, 704))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> directory.resolve("Vietnam", "Hanoi", "Springfield"))
                    .isInstanceOf(UnknownPlaceException.class)
                    .hasMessageContaining("city");
        }

        /**
         * The cascade, stated as a test rather than as a comment: a state lookup takes a country
         * id, so running it after the country failed would search within a country that does not
         * exist. Spreading these three calls across a caller is what made that possible.
         */
        @Test
        @DisplayName("nothing below an unresolved level is looked up")
        void doesNotLookUpBelowAFailure() {
            when(countries.findByNameIgnoreCase("Atlantis")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> directory.resolve("Atlantis", "Hanoi", "Ba Dinh"))
                    .isInstanceOf(UnknownPlaceException.class);

            verify(states, never()).findByNameIgnoreCaseAndCountryId(org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyInt());
            verify(cities, never()).findByNameIgnoreCaseAndStateIdAndCountryId(
                    org.mockito.ArgumentMatchers.any(),
                    org.mockito.ArgumentMatchers.anyInt(),
                    org.mockito.ArgumentMatchers.anyInt());
        }

        @Test
        @DisplayName("surrounding whitespace is trimmed before lookup")
        void trimsNames() {
            datasetHasVietnamHanoi();

            assertThat(directory.resolve("  Vietnam  ", null, null).countryId()).isEqualTo(704);
        }
    }

    // ---- listing -----------------------------------------------------------

    @Test
    @DisplayName("the dropdown lookups return published Places, not entities")
    void listsPlaces() {
        when(countries.findAllByOrderByNameAsc())
                .thenReturn(List.of(entity(Country.class, 704, "Vietnam")));
        when(states.findByCountryIdOrderByNameAsc(704))
                .thenReturn(List.of(entity(State.class, 3757, "Hanoi")));
        when(cities.findByCountryIdAndStateIdOrderByNameAsc(704, 3757))
                .thenReturn(List.of(entity(City.class, 21578, "Ba Dinh")));

        assertThat(directory.countries()).containsExactly(new Place(704, "Vietnam"));
        assertThat(directory.statesOf(704)).containsExactly(new Place(3757, "Hanoi"));
        assertThat(directory.citiesOf(704, 3757)).containsExactly(new Place(21578, "Ba Dinh"));
    }
}
