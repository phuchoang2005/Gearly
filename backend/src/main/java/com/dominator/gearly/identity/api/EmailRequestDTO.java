package com.dominator.gearly.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Request body carrying a single email address (resend verification, forgot password). */
@Getter
@Setter
public class EmailRequestDTO {

    @NotBlank
    @Email
    private String email;
}
