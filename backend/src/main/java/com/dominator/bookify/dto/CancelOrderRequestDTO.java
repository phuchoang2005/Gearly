package com.dominator.bookify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelOrderRequestDTO {
    @NotBlank(message = "Invalid Order.")
    private String orderId;

    @NotBlank(message = "Reason must not be blank.")
    private String reason;
}
