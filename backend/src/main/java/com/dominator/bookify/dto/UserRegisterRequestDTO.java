package com.dominator.bookify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserRegisterRequestDTO {

    @NotBlank
    String firstName;

    @NotBlank
    String lastName;

    @NotBlank
    private String fullName;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    private String phone;

    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
