package com.dominator.gearly.reviews.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One product's worth of a review submission.
 *
 * <p><b>The bounds on {@code rating} are the S12 half of a fix S11 started.</b> This field was
 * an unbounded {@code int}, and {@code applyRating} folded whatever arrived straight into the
 * product's running total — the S8 characterization suite pinned a review of <b>900 stars</b>
 * being accepted and permanently wrecking the average. S11 made that unrepresentable by routing
 * the rollup through the {@code Rating} value object, which turned it into a 400 carrying the
 * value object's own message. The plan assigns the remainder here: {@code @Min}/{@code @Max}
 * answer with a <em>field-level</em> error, so the storefront can point at the star selector
 * that is wrong instead of showing a sentence about a rating.
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateReviewRequestDTO {

    @NotBlank
    private String productId;

    private String subject;

    private String comment;

    @Min(value = 1, message = "a rating must be between 1 and 5")
    @Max(value = 5, message = "a rating must be between 1 and 5")
    private int rating;
}
