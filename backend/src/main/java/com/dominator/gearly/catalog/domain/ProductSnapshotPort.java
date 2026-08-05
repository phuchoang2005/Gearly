package com.dominator.gearly.catalog.domain;

import com.dominator.gearly.shared.domain.ProductId;

import java.util.List;
import java.util.Optional;

/**
 * <b>Catalog's published outbound interface.</b> The only door Cart and Ordering have into the
 * catalog, and it hands out {@link CatalogSnapshot}s rather than {@link Product}s.
 *
 * <p>Before this existed, {@code CartService} held a {@code ProductService} and
 * {@code PlaceOrderService} held one too, so two downstream contexts each had a reference to a
 * third context's application service and, through it, to its aggregate. The context map calls
 * that relationship customer/supplier with an anti-corruption layer; this interface is where
 * the layer actually is.
 *
 * <p>An interface in a {@code domain} package is published language under ArchUnit's
 * {@code contexts_touch_each_other_only_through_published_types}, which is what makes the
 * arrangement checkable rather than merely intended.
 */
public interface ProductSnapshotPort {

    /**
     * @throws ProductNotFoundException if there is no such product — the caller wanted this
     *         one and there is nothing sensible to return instead
     */
    CatalogSnapshot snapshotOf(ProductId productId);

    /** For callers that have something to do when a product has been delisted. */
    Optional<CatalogSnapshot> findSnapshot(ProductId productId);

    /**
     * One read for many ids. Missing products are simply absent from the result, so a caller
     * reconciling a stale list ({@code Cart.reconcileWith}) learns which of its lines no
     * longer point at anything.
     */
    List<CatalogSnapshot> snapshotsOf(List<ProductId> productIds);
}
