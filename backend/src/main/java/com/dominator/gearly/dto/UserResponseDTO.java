package com.dominator.gearly.dto;

import com.dominator.gearly.model.Address;
import com.dominator.gearly.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private String id;
    private String profileAvatar;
    private String firstName;
    private String lastName;
    private String fullName;
    private String email;
    private String phone;
    private List<String> favorites;
    private boolean verified;
    private UserStatus status;
    private Address address;
}
