package com.dominator.gearly.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonNameTest {

    /**
     * The point of the type. Today {@code UserService} computes {@code fullName} from the
     * parts while {@code AuthService.register} stores whatever the client sent, so the two
     * can disagree forever. Here there is only one answer.
     */
    @Test
    void derivesFullNameFromTheParts() {
        assertThat(new PersonName("Jane", "Doe").fullName()).isEqualTo("Jane Doe");
    }

    @Test
    void trimsSurroundingWhitespace() {
        PersonName name = new PersonName("  Jane  ", "  Doe  ");

        assertThat(name.firstName()).isEqualTo("Jane");
        assertThat(name.lastName()).isEqualTo("Doe");
        assertThat(name.fullName()).isEqualTo("Jane Doe");
    }

    @Test
    void rejectsBlankParts() {
        assertThatThrownBy(() -> new PersonName("", "Doe"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("first name must not be blank");
        assertThatThrownBy(() -> new PersonName("Jane", "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("last name must not be blank");
        assertThatThrownBy(() -> new PersonName(null, "Doe"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void parsesADisplayNameOnTheLastSpace() {
        assertThat(PersonName.parse("Jane Doe")).isEqualTo(new PersonName("Jane", "Doe"));
        // a multi-part given name keeps its parts together
        assertThat(PersonName.parse("Le Nguyen Hoang Phuc"))
                .isEqualTo(new PersonName("Le Nguyen Hoang", "Phuc"));
    }

    @Test
    void refusesToGuessAtASingleWordName() {
        assertThatThrownBy(() -> PersonName.parse("Cher"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must contain a first and last name");
    }

    @Test
    void buildsInitials() {
        assertThat(new PersonName("jane", "doe").initials()).isEqualTo("JD");
    }
}
