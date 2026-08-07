package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.DomainNotFoundException;

/**
 * The order or product a review would be about does not exist. Answers 404.
 *
 * <p>Two factories rather than two classes: a caller does nothing different for them, and the
 * message already says which. Both messages are carried over verbatim.
 */
public class ReviewSubjectNotFoundException extends DomainNotFoundException {

    private ReviewSubjectNotFoundException(String message) {
        super(message);
    }

    public static ReviewSubjectNotFoundException noSuchOrder() {
        return new ReviewSubjectNotFoundException(
                "Order not found, you cannot create review on this.");
    }

    public static ReviewSubjectNotFoundException noSuchProduct() {
        return new ReviewSubjectNotFoundException(
                "Product not found, you cannot create review for this product.");
    }
}
