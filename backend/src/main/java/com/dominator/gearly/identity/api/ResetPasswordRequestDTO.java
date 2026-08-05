package com.dominator.gearly.identity.api;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequestDTO {
    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;
}
