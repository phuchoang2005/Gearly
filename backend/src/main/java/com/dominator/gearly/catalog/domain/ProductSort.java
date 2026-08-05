package com.dominator.gearly.catalog.domain;

/**
 * The orders the storefront can ask a product listing to come back in.
 *
 * <p>Was a {@code switch} on a raw string inside {@code ProductService}, whose {@code default}
 * branch silently meant "alphabetical" — so a typo in the query parameter was indistinguishable
 * from asking for the default. The token is still translated leniently at the edge (an
 * unrecognized {@code sortBy} keeps its old meaning rather than becoming a new 400), but the
 * field the adapter sorts on is now decided in one place instead of at the call site.
 */
public enum ProductSort {

    NEWEST("addedAt", false),
    PRICE_ASC("price", true),
    PRICE_DESC("price", false),
    TITLE_ASC("title", true),
    TITLE_DESC("title", false),
    RATING_DESC("averageRating", false);

    private final String field;
    private final boolean ascending;

    ProductSort(String field, boolean ascending) {
        this.field = field;
        this.ascending = ascending;
    }

    /** The stored field this sorts on. Named here so the adapter does not have to know. */
    public String field() {
        return field;
    }

    public boolean isAscending() {
        return ascending;
    }

    /**
     * The storefront's token, or {@link #TITLE_ASC} for anything else.
     *
     * <p>Deliberately lenient, unlike {@code ProductCondition.fromWireValue}. An unrecognized
     * condition could only ever match nothing, so failing loudly told the caller something
     * true; an unrecognized sort has always just meant "alphabetical", and turning that into a
     * 400 would break bookmarked links for no gain.
     */
    public static ProductSort fromWireValue(String sortBy) {
        if (sortBy == null) {
            return TITLE_ASC;
        }
        return switch (sortBy) {
            case "newest" -> NEWEST;
            case "price-low" -> PRICE_ASC;
            case "price-high" -> PRICE_DESC;
            case "title-za" -> TITLE_DESC;
            default -> TITLE_ASC;
        };
    }
}
