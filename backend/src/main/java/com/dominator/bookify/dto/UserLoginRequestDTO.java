package com.dominator.bookify.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UserLoginRequestDTO {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

}