package com.dominator.gearly.reviews.application;

import java.util.List;

/**
 * What a customer submits: one order, and an opinion of each product in it.
 *
 * <p>The application layer's own vocabulary rather than the request DTO's, so the use case can
 * be exercised without building a JSON body — the split S10 made for every ordering command.
 */
public record SubmitReviewsCommand(String orderId, List<Line> lines) {

    public record Line(String productId, String subject, String comment, int rating) {
    }
}
