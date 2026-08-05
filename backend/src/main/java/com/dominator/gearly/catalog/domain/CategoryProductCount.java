package com.dominator.gearly.catalog.domain;

/**
 * A category and how many products sit under it — the shape the storefront's navigation needs.
 *
 * <p>A read model rather than an aggregate: it is assembled by an aggregation that joins two
 * collections, and nothing can be changed through it. It lives in the domain because the
 * {@link CategoryRepository} port is expressed in terms of it, and the port may not name a
 * response DTO.
 */
public record CategoryProductCount(String id, String name, int productCount) {
}
