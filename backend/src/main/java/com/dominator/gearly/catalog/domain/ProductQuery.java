package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.ProductCondition;

import java.util.List;

/**
 * What the storefront is asking the catalog for: the facets, the sort and the page.
 *
 * <p>The request DTO stops at {@code catalog.api}; this is what the use case passes inward, so
 * the adapter never sees a query string and the domain never names one. Every optional facet
 * has an {@code has*} predicate rather than the adapter re-deciding what {@code null} or blank
 * means.
 *
 * <p>{@code minPrice}, {@code maxPrice} and {@code minRating} stay primitive doubles rather
 * than becoming {@code Money} and {@code Rating}: they are inclusive bounds on a query string,
 * not amounts anyone is charged or stars anyone gave, and {@code Rating}'s 1–5 invariant would
 * reject the {@code 0} the storefront sends to mean "no rating filter". S9 reached the same
 * conclusion and recorded it.
 */
public record ProductQuery(ProductCondition condition,
                           double minPrice,
                           double maxPrice,
                           List<CategoryId> categoryIds,
                           String searchTerm,
                           double minRating,
                           ProductSort sort,
                           int page,
                           int size) {

    public ProductQuery {
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
        sort = sort == null ? ProductSort.TITLE_ASC : sort;
    }

    public boolean hasCondition() {
        return condition != null;
    }

    public boolean hasCategories() {
        return !categoryIds.isEmpty();
    }

    public boolean hasSearchTerm() {
        return searchTerm != null && !searchTerm.isBlank();
    }

    public boolean hasMinRating() {
        return minRating > 0;
    }
}
