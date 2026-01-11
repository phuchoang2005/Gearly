package com.dominator.bookify.dto;

import com.dominator.bookify.model.ShippingInformation;
import lombok.Getter;
import lombok.Setter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
public class OrderCreationRequestDTO {
    @NotEmpty(message = "Order must contain at least one item.")
    @Valid
    private List<OrderItemRequestDTO> items;

    @NotNull(message = "Payment information is required.")
    @Valid
    private PaymentRequestDTO paymentInfo;

    @NotNull(message = "Shipping address is required.")
    @Valid
    private ShippingInformation shippingInformation;
}