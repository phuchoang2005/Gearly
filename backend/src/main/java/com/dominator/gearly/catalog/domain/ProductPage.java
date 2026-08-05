package com.dominator.gearly.catalog.domain;

import java.util.List;

/**
 * One page of catalog results, as a value the domain can express.
 *
 * <p>The counterpart of {@code OrderPage}, and it exists for the same reason: Spring Data's
 * {@code Page} lives in {@code org.springframework.data.domain}, which ArchUnit's
 * {@code domain_is_free_of_framework_types} bans from a domain package. The application layer
 * turns this into the {@code Page} the controllers still return, so no response shape changes.
 */
public record ProductPage(List<Product> content, int page, int size, long totalElements) {

    public ProductPage {
        content = List.copyOf(content);
    }
}
