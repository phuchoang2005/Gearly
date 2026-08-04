package com.dominator.gearly.dto;

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

    /**
     * @deprecated Ignored since S9 — the stored full name is derived from
     *         {@code firstName} and {@code lastName} by {@code PersonName}. Honouring a
     *         client-supplied value here is what let a registration create a user whose
     *         display name disagreed with its own name parts. Still accepted (and still
     *         sent by both frontends) so existing clients keep working; no longer
     *         {@code @NotBlank}, so a client that drops it works too.
     */
    @Deprecated
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
