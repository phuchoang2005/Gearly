package com.dominator.gearly.catalog.application;

import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductNotFoundException;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.ordering.domain.OrderCancelled;
import com.dominator.gearly.ordering.domain.OrderPlaced;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What the catalog does when an order happens: units come off the shelf when one is placed,
 * and go back on it when one is cancelled.
 *
 * <p>This is half of what {@code ordering.application.OrderPlacedListener} used to be. The
 * split is not tidying. That class held a {@code ProductService} <em>and</em> a
 * {@code CartService}, which made the ordering context the place that knew how to change two
 * other contexts' state — a distributed monolith with events bolted on the front. Each context
 * now listens for itself, and ArchUnit enforces that it can only do so through a published
 * event.
 *
 * <h2>Why {@code BEFORE_COMMIT} for placement</h2>
 * Because the stock decrement has to be atomic with the order. {@code AFTER_COMMIT} would put
 * it outside the transaction, so a failure here would leave an order placed against stock that
 * was never taken — overselling the next customer. {@code BEFORE_COMMIT} runs inside the
 * caller's transaction, so an exception here rolls the order back with it. That is the property
 * {@code OrderPlacementTransactionIntegrationTest} proves against a real replica set, and S10
 * proved the phase is load-bearing by switching it to {@code AFTER_COMMIT} and watching the
 * rollback test fail.
 *
 * <p>The alternative reading — that these are separate aggregates and so belong in separate
 * transactions with eventual consistency — is the right one for a system that can tolerate
 * temporary oversell. This one cannot: it has a single database and a hard stock invariant, so
 * the honest answer is one transaction and a clearly documented reason.
 *
 * <h2>The cancellation half is new behavior</h2>
 * Nothing put cancelled units back before. An order placed for the last three of something and
 * then cancelled left the catalog believing it had none, permanently, with no way back except
 * an administrator editing the stock by hand. S10 found this, named it a real bug, and left it
 * for the sprint that would own {@code Product.restock}. This is that sprint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogStockListener {

    private final ProductRepository products;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(OrderPlaced event) {
        List<Product> touched = new ArrayList<>();
        for (Map.Entry<ProductId, Quantity> line : event.quantities().entrySet()) {
            Product product = require(line.getKey());
            product.reserve(line.getValue());
            touched.add(product);
        }
        products.saveAll(touched);
    }

    /**
     * Cancelling returns the units.
     *
     * <p>{@code BEFORE_COMMIT} again, for the same reason and one more: the cancellation and
     * the restock describe the same fact, and a system that recorded one without the other
     * would be reporting stock it does not have. A product deleted since the order was placed
     * is skipped with a warning rather than failing the cancellation — the customer's
     * cancellation must not be held hostage to a catalog row that no longer exists, and there
     * is nothing to put the units back on.
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(OrderCancelled event) {
        List<Product> touched = new ArrayList<>();
        for (Map.Entry<ProductId, Quantity> line : event.quantities().entrySet()) {
            products.findById(line.getKey()).ifPresentOrElse(
                    product -> {
                        product.restock(line.getValue());
                        touched.add(product);
                    },
                    () -> log.warn("Cancelled order {} referenced product {}, which no longer "
                                    + "exists; its {} units cannot be restocked",
                            event.orderId(), line.getKey(), line.getValue()));
        }
        products.saveAll(touched);
    }

    private Product require(ProductId productId) {
        return products.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }
}
