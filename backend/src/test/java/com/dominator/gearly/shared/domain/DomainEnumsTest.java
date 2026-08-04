package com.dominator.gearly.shared.domain;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DomainEnumsTest {

    @Nested
    class Roles {

        @Test
        void buildsTheSpringSecurityAuthority() {
            // exactly what "ROLE_" + user.getRole() produced before
            assertThat(Role.CUSTOMER.authority()).isEqualTo("ROLE_CUSTOMER");
            assertThat(Role.ADMIN.authority()).isEqualTo("ROLE_ADMIN");
        }

        @Test
        void defaultsToCustomer() {
            assertThat(Role.DEFAULT).isEqualTo(Role.CUSTOMER);
        }

        @Test
        void parsesTheStoredValuesCaseInsensitively() {
            assertThat(Role.fromValue("ADMIN")).isEqualTo(Role.ADMIN);
            assertThat(Role.fromValue("customer")).isEqualTo(Role.CUSTOMER);
            assertThat(Role.fromValue(null)).isNull();
        }

        @Test
        void rejectsAnUnknownRoleLoudly() {
            assertThatThrownBy(() -> Role.fromValue("SUPERADMIN"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown role");
        }
    }

    @Nested
    class Conditions {

        /**
         * The storefront's filter set and colour map both key on {@code "LIKE NEW"} with a
         * space ({@code ConditionFilter.jsx}, {@code ConditionTag.jsx}). Writing
         * {@code LIKE_NEW} would match neither.
         */
        @Test
        void keepsTheSpacedWireValueRatherThanTheConstantName() {
            assertThat(ProductCondition.LIKE_NEW.wireValue()).isEqualTo("LIKE NEW");
            assertThat(ProductCondition.LIKE_NEW.toString()).isEqualTo("LIKE NEW");
            assertThat(ProductCondition.LIKE_NEW.name()).isEqualTo("LIKE_NEW");
        }

        @Test
        void coversTheVocabularyTheStorefrontOffers() {
            assertThat(ProductCondition.values())
                    .extracting(ProductCondition::wireValue)
                    .containsExactly("NEW", "LIKE NEW", "GOOD", "ACCEPTABLE");
        }

        @Test
        void parsesTheStoredValues() {
            assertThat(ProductCondition.fromWireValue("NEW")).isEqualTo(ProductCondition.NEW);
            assertThat(ProductCondition.fromWireValue("LIKE NEW")).isEqualTo(ProductCondition.LIKE_NEW);
            assertThat(ProductCondition.fromWireValue("acceptable")).isEqualTo(ProductCondition.ACCEPTABLE);
            assertThat(ProductCondition.fromWireValue(null)).isNull();
        }

        /**
         * The old code compared the filter value to the stored one with {@code String}
         * equality, so an unrecognized condition matched nothing and reported no error.
         */
        @Test
        void rejectsAnUnknownConditionLoudly() {
            assertThatThrownBy(() -> ProductCondition.fromWireValue("MINT"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown product condition");
        }
    }
}
