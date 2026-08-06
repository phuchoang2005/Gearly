package com.dominator.gearly.reviews.domain;

import java.util.List;

/**
 * One page of reviews, in the domain's own terms.
 *
 * <p>The same shape as {@code OrderPage} and {@code ProductPage}, and there for the same
 * reason: {@code org.springframework.data.domain} is banned from a domain package, so the port
 * cannot return a Spring {@code Page}. The api layer rebuilds one at the boundary, where the
 * JSON both frontends read is decided.
 */
public record ReviewPage(List<Review> content, int page, int size, long totalElements) {
}
