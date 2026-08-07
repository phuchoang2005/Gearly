package com.dominator.gearly.content.domain;

import com.dominator.gearly.shared.domain.DomainNotFoundException;

/**
 * An article or page that does not exist. Answers 404, as the {@code ResourceNotFoundException}
 * it replaces did — but stated by the context that knows what is missing, and without the
 * domain naming the legacy {@code exception} package.
 */
public class ContentNotFoundException extends DomainNotFoundException {

    private ContentNotFoundException(String message) {
        super(message);
    }

    public static ContentNotFoundException blogPost(String id) {
        return new ContentNotFoundException("Blog post not found with ID: " + id);
    }

    public static ContentNotFoundException page(String slug) {
        return new ContentNotFoundException("Static page not found with slug: " + slug);
    }
}
