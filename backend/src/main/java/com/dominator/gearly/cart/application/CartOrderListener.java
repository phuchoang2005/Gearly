package com.dominator.gearly.cart.application;

import com.dominator.gearly.ordering.domain.OrderPlaced;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * What the cart does when an order is placed: the units the customer just bought come out of
 * their basket, and anything they left behind stays.
 *
 * <p>The other half of what {@code ordering.application.OrderPlacedListener} used to be — see
 * {@code CatalogStockListener} for why one class holding both was the wrong shape, and for the
 * reasoning behind {@code BEFORE_COMMIT}.
 *
 * <p>The event's {@code Map<ProductId, Quantity>} is exactly the shape {@code Cart.removeUnits}
 * wants, so nothing is translated on the way through. It used to be built here from the
 * order's lines with a {@code Collectors.toMap} that threw on a product appearing twice.
 */
@Component
@RequiredArgsConstructor
public class CartOrderListener {

    private final CartService cartService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(OrderPlaced event) {
        cartService.removeItems(event.userId(), null, event.quantities());
    }
}
