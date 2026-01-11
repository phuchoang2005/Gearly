package com.dominator.bookify.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class OrderItemRequestDTO {
    @NotBlank(message = "Book ID cannot be blank.")
    private String bookId;

    @Min(value = 1, message = "Quantity must be at least 1.")
    private int quantity;
}