package com.dominator.gearly.shared.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The string-backed value objects: {@link EmailAddress}, {@link PhoneNumber}, {@link Slug}. */
class TextValueObjectsTest {

    @Nested
    class Emails {

        @ParameterizedTest
        @ValueSource(strings = {
                "kaxe2018@gmail.com",
                "first.last@sub.example.co.uk",
                "user+tag@example.com"
        })
        void acceptsRealisticAddresses(String address) {
            assertThat(new EmailAddress(address).value()).isEqualTo(address);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "no-at-sign",
                "@example.com",
                "user@",
                "user@localhost",
                "two@@example.com",
                "spaced out@example.com",
                "user@example."
        })
        void rejectsMalformedAddresses(String address) {
            assertThatThrownBy(() -> new EmailAddress(address))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a valid email address");
        }

        @Test
        void rejectsNull() {
            assertThatThrownBy(() -> new EmailAddress(null)).isInstanceOf(NullPointerException.class);
        }

        /**
         * S9's contract is a byte-identical document. Lower-casing on construction would
         * rewrite stored addresses that carry a unique index — deferred to S12 as a real
         * migration, so the stored form must survive verbatim.
         */
        @Test
        void preservesCaseRatherThanNormalizingIt() {
            EmailAddress mixedCase = new EmailAddress("Jane.Doe@Example.COM");

            assertThat(mixedCase.value()).isEqualTo("Jane.Doe@Example.COM");
            assertThat(mixedCase.toString()).isEqualTo("Jane.Doe@Example.COM");
            assertThat(mixedCase.normalized()).isEqualTo("jane.doe@example.com");
        }

        @Test
        void exposesTheDomain() {
            assertThat(new EmailAddress("a@b.example.com").domain()).isEqualTo("b.example.com");
        }
    }

    @Nested
    class PhoneNumbers {

        @ParameterizedTest
        @ValueSource(strings = {"0912345678", "0912 345 678", "+84 912-345-678", "(028) 3822 1234"})
        void acceptsTheFormatsAlreadyInTheData(String number) {
            assertThat(new PhoneNumber(number).value()).isEqualTo(number);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "12345", "not a phone", "+84-912-345-678-901-234"})
        void rejectsValuesThatAreNotPhoneNumbers(String number) {
            assertThatThrownBy(() -> new PhoneNumber(number))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a valid phone number");
        }

        @Test
        void reducesToDigitsForComparisonWithoutChangingTheStoredForm() {
            PhoneNumber number = new PhoneNumber("+84 912-345-678");

            assertThat(number.digitsOnly()).isEqualTo("+84912345678");
            assertThat(number.value()).isEqualTo("+84 912-345-678");
        }
    }

    @Nested
    class Slugs {

        @Test
        void acceptsAnAlreadyValidSlug() {
            assertThat(new Slug("graphics-cards").value()).isEqualTo("graphics-cards");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "Graphics Cards", "graphics_cards", "-leading", "trailing-", "double--hyphen"})
        void rejectsAnythingThatIsNotAlreadyASlug(String value) {
            assertThatThrownBy(() -> new Slug(value))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not a valid slug");
        }

        @Test
        void derivesASlugFromFreeText() {
            assertThat(Slug.from("Graphics Cards").value()).isEqualTo("graphics-cards");
            assertThat(Slug.from("  Intel Core i5-12400F  ").value()).isEqualTo("intel-core-i5-12400f");
        }

        /** The catalog carries Vietnamese product and category names. */
        @Test
        void stripsVietnameseDiacritics() {
            assertThat(Slug.from("Bàn phím cơ").value()).isEqualTo("ban-phim-co");
            assertThat(Slug.from("Ổ đĩa cứng").value()).isEqualTo("o-dia-cung");
        }

        @Test
        void refusesTextWithNothingSluggableInIt() {
            assertThatThrownBy(() -> Slug.from("!!!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cannot derive a slug");
        }
    }
}
