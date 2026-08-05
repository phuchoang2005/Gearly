package com.dominator.gearly.cart.application;

import com.dominator.gearly.ordering.domain.OrderPlaced;
import com.dominator.gearly.service.user.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What the cart does when an order is placed: the units the customer just bought come out of
 * their basket, and anything they left behind stays.
 *
 * <p>The other half of what {@code ordering.application.OrderPlacedListener} used to be — see
 * {@code CatalogStockListener} for why one class holding both was the wrong shape, and for the
 * reasoning behind {@code BEFORE_COMMIT}.
 */
@Component
@RequiredArgsConstructor
public class CartOrderListener {

    private final CartService cartService;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(OrderPlaced event) {
        Map<String, Integer> bought = new LinkedHashMap<>();
        event.quantities().forEach((productId, quantity) ->
                bought.put(productId.value(), quantity.toInt()));

        cartService.removeItems(event.userId().value(), null, bought);
    }
}
