package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderPlaced;
import com.dominator.gearly.service.user.CartService;
import com.dominator.gearly.service.user.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * What happens elsewhere when an order is placed: the catalog's stock comes down, and the
 * buyer's cart is emptied of what they just bought.
 *
 * <h2>Why {@code BEFORE_COMMIT} and not {@code AFTER_COMMIT}</h2>
 * Because the stock decrement has to be atomic with the order. {@code AFTER_COMMIT} would put
 * it outside the transaction, so a failure there would leave an order placed against stock
 * that was never taken — overselling the next customer. {@code BEFORE_COMMIT} runs inside the
 * caller's transaction, so an exception here rolls the order back with it. That is the
 * property {@code OrderPlacementTransactionIntegrationTest} proves against a real replica set,
 * and it proves it by injecting the failure into the cart clear — the second of the two steps,
 * so the stock write has already succeeded when it blows up.
 *
 * <p>The alternative reading — that these are separate aggregates and so belong in separate
 * transactions with eventual consistency — is the right one for a system that can tolerate
 * temporary oversell. This one cannot: it has a single database and a hard stock invariant, so
 * the honest answer is one transaction and a clearly documented reason.
 *
 * <p>What this changes and what it does not: exactly the same two calls happen, in the same
 * order, inside the same transaction. What moved is <em>who decides</em>. Placement no longer
 * names Catalog or Cart; it announces what happened and this listener — which is allowed to
 * know about both — reacts. S11 replaces the {@code ProductService} call with
 * {@code Product.reserve(Quantity)} behind the catalog's own port.
 */
@Component
@RequiredArgsConstructor
public class OrderPlacedListener {

    private final ProductService productService;
    private final CartService cartService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(OrderPlaced event) {
        for (OrderLine line : event.lines()) {
            productService.decreaseStock(line.getProductId().value(), line.getQuantity().toInt());
        }

        Map<String, Integer> quantitiesByProduct = event.lines().stream()
                .collect(Collectors.toMap(
                        line -> line.getProductId().value(),
                        line -> line.getQuantity().toInt()));
        cartService.removeItems(event.userId().value(), null, quantitiesByProduct);
    }
}
