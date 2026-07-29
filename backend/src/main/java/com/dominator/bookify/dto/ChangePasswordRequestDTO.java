package com.dominator.bookify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Request body for an authenticated password change. */
@Getter
@Setter
public class ChangePasswordRequestDTO {

    @NotBlank
    private String oldPassword;

    @NotBlank
    private String newPassword;
}
