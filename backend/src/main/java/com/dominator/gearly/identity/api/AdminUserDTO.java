package com.dominator.gearly.identity.api;

import com.dominator.gearly.shared.domain.Address;
import com.dominator.gearly.identity.domain.UserStatus;
import com.dominator.gearly.shared.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDTO {
    private String id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private Address address;
    private Role role;
    private boolean verified;
    private List<String> favorites;
    private Instant createdAt;
    private Instant updatedAt;
    private UserStatus status;
}
