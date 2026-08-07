package com.dominator.gearly.catalog.domain;

import java.util.List;

/**
 * Catalog's published search, for contexts that need to find products by phrase rather than by
 * id.
 *
 * <p>The assistant is the caller. Before S13 it held {@code catalog.application.
 * ProductQueryService} and a {@code catalog.api.ProductSummaryDTO} — an application service and
 * a response DTO of another context, which is the coupling
 * {@code contexts_touch_each_other_only_through_published_types} exists to refuse. Worse than
 * the rule violation: it meant a change to the storefront's product-list JSON could break the
 * chatbot.
 */
public interface ProductSearchPort {

    /**
     * Products whose title contains {@code phrase}, best-effort and capped.
     *
     * <p>Empty for a blank phrase and for no matches — a search that finds nothing is not an
     * error, and the caller has something to say either way.
     */
    List<ProductBrief> searchByTitle(String phrase);
}
