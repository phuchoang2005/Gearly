package com.dominator.gearly.catalog.application;

import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductBrief;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.catalog.domain.ProductSearchPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * The bean behind {@link ProductSearchPort}, in the same shape as {@link CatalogSnapshotService}.
 *
 * <p>The five-result cap and the blank-phrase short circuit are carried over verbatim from
 * {@code ProductQueryService.findByTitle}, which is what the assistant used to call directly.
 */
@Service
@RequiredArgsConstructor
public class ProductSearchService implements ProductSearchPort {

    /** As many as fit in a chat reply's product list without becoming a wall of text. */
    private static final int MAX_RESULTS = 5;

    private final ProductRepository products;

    @Override
    public List<ProductBrief> searchByTitle(String phrase) {
        if (phrase == null || phrase.isBlank()) {
            return List.of();
        }
        return products.findByTitleContaining(phrase.trim()).stream()
                .limit(MAX_RESULTS)
                .map(ProductSearchService::brief)
                .toList();
    }

    private static ProductBrief brief(Product product) {
        return new ProductBrief(
                product.productId(),
                product.getTitle(),
                product.getPrice(),
                product.getAverageRating());
    }
}
