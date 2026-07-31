package com.dominator.bookify.mapper;

import com.dominator.bookify.dto.AdminUserDTO;
import com.dominator.bookify.dto.UserResponseDTO;
import com.dominator.bookify.model.User;
import org.springframework.stereotype.Component;

/**
 * Maps {@link User} entities to their public and admin-facing DTOs.
 * Never exposes the password hash.
 */
@Component
public class UserMapper {

    /** Customer-facing profile view (login response, profile screen). */
    public UserResponseDTO toResponseDto(User user) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setProfileAvatar(user.getProfileAvatar());
        dto.setFullName(user.getFullName());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFavorites(user.getFavorites());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setVerified(user.isVerified());
        dto.setStatus(user.getStatus());
        dto.setAddress(user.getAddress());
        return dto;
    }

    /** Admin console view: adds role and audit timestamps, omits the password hash. */
    public AdminUserDTO toAdminDto(User user) {
        AdminUserDTO dto = new AdminUserDTO();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setRole(user.getRole());
        dto.setVerified(user.isVerified());
        dto.setFavorites(user.getFavorites());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setStatus(user.getStatus());
        return dto;
    }
}
