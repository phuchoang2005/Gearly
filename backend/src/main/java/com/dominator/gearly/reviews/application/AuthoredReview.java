package com.dominator.gearly.reviews.application;

import com.dominator.gearly.reviews.domain.Review;

/**
 * A review together with the name to print beside it.
 *
 * <p>The author's name is not on the aggregate and must not be: it belongs to identity, changes
 * without the review changing, and is resolved through {@code UserDirectory}. Pairing the two
 * here rather than in the mapper keeps the api layer free of the "who wrote this" lookup, and
 * keeps the batched version of that lookup — one query for a page of reviews — in the layer
 * that knows how many rows it is about to render.
 */
public record AuthoredReview(Review review, String authorName) {
}
