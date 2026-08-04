package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.UserId;

import java.util.Objects;

/**
 * What a customer is asking to see of their own order history: an optional status filter, an
 * optional free-text term, and which page.
 *
 * <p>Always scoped to one {@link UserId} — the field is required, so there is no way to
 * express "everyone's orders" through this type. The customer order list is the endpoint an
 * unscoped query would turn into a data leak, so the scoping is structural rather than a
 * criterion the adapter has to remember to add.
 *
 * @param status     null means "any status"
 * @param searchTerm null or blank means "no term"
 */
public record OrderQuery(UserId userId, OrderStatus status, String searchTerm, int page, int size) {

    public OrderQuery {
        Objects.requireNonNull(userId, "an order query is always scoped to one user");
        if (page < 0) {
            throw new IllegalArgumentException("page must not be negative, was " + page);
        }
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1, was " + size);
        }
        searchTerm = (searchTerm == null || searchTerm.isBlank()) ? null : searchTerm.trim();
    }

    public boolean hasSearchTerm() {
        return searchTerm != null;
    }

    public boolean hasStatus() {
        return status != null;
    }
}
