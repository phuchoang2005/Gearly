package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.DomainNotFoundException;

/** No such review. 404, with the message the moderation console has always shown. */
public class ReviewNotFoundException extends DomainNotFoundException {

    public ReviewNotFoundException() {
        super("Review not found");
    }
}
