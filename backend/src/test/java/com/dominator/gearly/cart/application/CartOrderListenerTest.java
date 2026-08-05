package com.dominator.gearly.cart.application;

import com.dominator.gearly.ordering.domain.OrderPlaced;
import com.dominator.gearly.service.user.CartService;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

/**
 * The cart's half of what {@code OrderPlacedListenerTest} covered: only the units actually
 * bought leave the basket, and anything else the customer left in it stays.
 */
@ExtendWith(MockitoExtension.class)
class CartOrderListenerTest {

    @Mock private CartService cartService;

    private CartOrderListener listener;

    private static final UserId BUYER = UserId.of("user-1");

    @BeforeEach
    void setUp() {
        listener = new CartOrderListener(cartService);
    }

    @Test
    @DisplayName("the bought quantities are removed from the buyer's cart, keyed by product")
    void removesTheBoughtQuantities() {
        listener.on(new OrderPlaced(BUYER, Map.of(
                ProductId.of("p1"), Quantity.of(2),
                ProductId.of("p2"), Quantity.of(1)),
                Money.of(100.0), Instant.now()));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> captor = ArgumentCaptor.forClass(Map.class);
        verify(cartService).removeItems(eq(BUYER.value()), isNull(), captor.capture());

        assertThat(captor.getValue()).containsOnly(entry("p1", 2), entry("p2", 1));
    }
}
