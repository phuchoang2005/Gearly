package com.dominator.gearly.identity.api;

import com.dominator.gearly.identity.domain.User;
import com.dominator.gearly.identity.domain.UserFixture;
import com.dominator.gearly.identity.domain.UserStatus;
import com.dominator.gearly.shared.domain.Address;
import com.dominator.gearly.shared.domain.Role;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class UserResponseMapperTest {

    private final UserResponseMapper mapper = new UserResponseMapper();

    private User sampleUser() {
        User user = UserFixture.aUser()
                .named("Ada", "Lovelace")
                .withEmail("ada@example.com")
                .withPhone("123456")
                .at(new Address("1 St", "City", 1, "State", 2, "0000", "Country", 3))
                .asAdmin()
                .favouring("b1", "b2")
                .persistedAs("u1", Instant.parse("2025-01-01T00:00:00Z"),
                        Instant.parse("2025-02-01T00:00:00Z"))
                .build();
        user.changeAvatar("/uploads/avatars/u1.png");
        return user;
    }

    @Test
    void toResponseDto_copiesPublicFields() {
        UserResponseDTO dto = mapper.toResponseDto(sampleUser());

        assertThat(dto.getId()).isEqualTo("u1");
        assertThat(dto.getProfileAvatar()).isEqualTo("/uploads/avatars/u1.png");
        assertThat(dto.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(dto.getEmail()).isEqualTo("ada@example.com");
        assertThat(dto.getPhone()).isEqualTo("123456");
        assertThat(dto.getFavorites()).containsExactly("b1", "b2");
        assertThat(dto.isVerified()).isTrue();
        assertThat(dto.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(dto.getAddress()).isNotNull();
        // UserResponseDTO exposes no password field, so the hash cannot leak through it.
    }

    @Test
    void toAdminDto_copiesAdminFieldsAndTimestamps() {
        AdminUserDTO dto = mapper.toAdminDto(sampleUser());

        assertThat(dto.getId()).isEqualTo("u1");
        assertThat(dto.getRole()).isEqualTo(Role.ADMIN);
        assertThat(dto.getEmail()).isEqualTo("ada@example.com");
        assertThat(dto.getFavorites()).containsExactly("b1", "b2");
        assertThat(dto.isVerified()).isTrue();
        assertThat(dto.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(dto.getCreatedAt()).isEqualTo(Instant.parse("2025-01-01T00:00:00Z"));
        assertThat(dto.getUpdatedAt()).isEqualTo(Instant.parse("2025-02-01T00:00:00Z"));
    }

    /**
     * The value objects the aggregate holds are unwrapped, so the JSON is the same strings it
     * has always been.
     *
     * <p>This is the S9 lesson applied to identity: a DTO-equals-entity comparison would pass
     * whatever shape {@code EmailAddress} happened to serialize as, because it would read the
     * same way on both sides. The literal keys and values are what the frontends parse, so they
     * are what is asserted — {@code "ada@example.com"}, a bare string, not {@code {"value": …}}.
     */
    @Test
    @DisplayName("email, phone and favourites serialize as the plain strings they always were")
    void valueObjectsAreUnwrappedOnTheWire() throws Exception {
        JsonNode json = new ObjectMapper().valueToTree(mapper.toResponseDto(sampleUser()));

        assertThat(json.get("email").isTextual()).isTrue();
        assertThat(json.get("email").asText()).isEqualTo("ada@example.com");
        assertThat(json.get("phone").isTextual()).isTrue();
        assertThat(json.get("phone").asText()).isEqualTo("123456");
        assertThat(json.get("favorites").isArray()).isTrue();
        assertThat(json.get("favorites").get(0).asText()).isEqualTo("b1");
        assertThat(json.has("passwordHash")).isFalse();
    }
}
