package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.AdminUserDTO;
import com.dominator.gearly.dto.UserResponseDTO;
import com.dominator.gearly.model.Address;
import com.dominator.gearly.model.User;
import com.dominator.gearly.model.UserStatus;
import com.dominator.gearly.shared.domain.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    private User sampleUser() {
        User u = new User();
        u.setId("u1");
        u.setProfileAvatar("/uploads/avatars/u1.png");
        u.setFirstName("Ada");
        u.setLastName("Lovelace");
        u.setFullName("Ada Lovelace");
        u.setEmail("ada@example.com");
        u.setPasswordHash("secret-hash");
        u.setPhone("123");
        u.setAddress(new Address("1 St", "City", 1, "State", 2, "0000", "Country", 3));
        u.setRole(Role.ADMIN);
        u.setVerified(true);
        u.setFavorites(List.of("b1", "b2"));
        u.setStatus(UserStatus.ACTIVE);
        u.setCreatedAt(Instant.parse("2025-01-01T00:00:00Z"));
        u.setUpdatedAt(Instant.parse("2025-02-01T00:00:00Z"));
        return u;
    }

    @Test
    void toResponseDto_copiesPublicFields() {
        UserResponseDTO dto = mapper.toResponseDto(sampleUser());

        assertThat(dto.getId()).isEqualTo("u1");
        assertThat(dto.getProfileAvatar()).isEqualTo("/uploads/avatars/u1.png");
        assertThat(dto.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(dto.getEmail()).isEqualTo("ada@example.com");
        assertThat(dto.getPhone()).isEqualTo("123");
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
}
