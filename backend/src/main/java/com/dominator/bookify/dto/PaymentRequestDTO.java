package com.dominator.bookify.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class PaymentRequestDTO {
    @NotBlank(message = "Payment method is required.")
    private String method;
}