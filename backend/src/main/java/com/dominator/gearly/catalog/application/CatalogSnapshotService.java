package com.dominator.gearly.catalog.application;

import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductNotFoundException;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.catalog.domain.ProductSnapshotPort;
import com.dominator.gearly.shared.domain.ProductId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Catalog's side of the anti-corruption layer: loads a {@link Product} and hands out only
 * {@link CatalogSnapshot}.
 *
 * <p>Small on purpose. All the interesting decisions — which fields are published, what
 * happens to an image-less product, what "can this supply N units" means — live on the
 * aggregate and on the snapshot, where a unit test can reach them without a Spring context.
 * This class exists to be the bean that satisfies the port.
 */
@Service
@RequiredArgsConstructor
public class CatalogSnapshotService implements ProductSnapshotPort {

    private final ProductRepository products;

    @Override
    public CatalogSnapshot snapshotOf(ProductId productId) {
        return products.findById(productId)
                .map(Product::snapshot)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Override
    public Optional<CatalogSnapshot> findSnapshot(ProductId productId) {
        return products.findById(productId).map(Product::snapshot);
    }

    @Override
    public List<CatalogSnapshot> snapshotsOf(List<ProductId> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return products.findAllById(productIds).stream()
                .map(Product::snapshot)
                .toList();
    }
}
