package com.dominator.gearly.reviews.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A customer's reviews of one order, submitted together.
 *
 * <p>{@code @Valid} on the list is load-bearing: without it the per-line bounds on
 * {@link CreateReviewRequestDTO}'s rating are never evaluated, because bean validation does not
 * descend into a collection's elements unless it is told to. The bounds would have looked
 * present and done nothing.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateReviewsRequestDTO {

    @NotBlank
    private String orderId;

    @NotEmpty
    @Valid
    private List<CreateReviewRequestDTO> reviews;
}
