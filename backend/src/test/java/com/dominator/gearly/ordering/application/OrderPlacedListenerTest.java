package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderPlaced;
import com.dominator.gearly.service.user.CartService;
import com.dominator.gearly.service.user.ProductService;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

/**
 * <b>Characterization suite (S8), carried forward.</b> The stock decrement and the cart clear
 * used to be two private method calls at the end of {@code createOrder}. They are the same two
 * calls, in the same order, inside the same transaction — reached through {@link OrderPlaced}
 * instead of by placement naming two other contexts directly.
 */
@ExtendWith(MockitoExtension.class)
class OrderPlacedListenerTest {

    @Mock private ProductService productService;
    @Mock private CartService cartService;

    private OrderPlacedListener listener;

    private static final UserId USER_ID = UserId.of("user-1");

    @BeforeEach
    void setUp() {
        listener = new OrderPlacedListener(productService, cartService);
    }

    private OrderPlaced placed(int quantity) {
        return new OrderPlaced(USER_ID,
                List.of(OrderFixture.line("p1", "Product p1", 10.00, quantity)),
                Money.of(36.60),
                Instant.now());
    }

    @Test
    @DisplayName("placement decrements stock and removes the ordered quantities from the cart")
    void appliesStockAndClearsCart() {
        listener.on(placed(2));

        verify(productService).decreaseStock("p1", 2);
        verify(cartService).removeItems(eq("user-1"), isNull(), eq(Map.of("p1", 2)));
    }

    @Test
    @DisplayName("every line is decremented, not just the first")
    void decrementsEveryLine() {
        listener.on(new OrderPlaced(USER_ID,
                List.of(OrderFixture.line("p1", "GPU", 10.00, 2),
                        OrderFixture.line("p2", "CPU", 20.00, 1)),
                Money.of(51.60),
                Instant.now()));

        verify(productService).decreaseStock("p1", 2);
        verify(productService).decreaseStock("p2", 1);
        verify(cartService).removeItems(eq("user-1"), isNull(), eq(Map.of("p1", 2, "p2", 1)));
    }

    /**
     * The ordering matters to the transaction test, which injects its failure into the cart
     * clear precisely because the stock write has already succeeded by then. If these ever
     * swapped, that test would still pass while proving something much weaker.
     */
    @Test
    @DisplayName("stock comes down before the cart is cleared")
    void decrementsStockFirst() {
        listener.on(placed(2));

        InOrder order = inOrder(productService, cartService);
        order.verify(productService).decreaseStock("p1", 2);
        order.verify(cartService).removeItems(eq("user-1"), isNull(), anyMap());
    }

    /**
     * Swallowing this would be the worst possible outcome: the order commits, the stock write
     * commits, and the cart silently keeps items the customer has already paid for — or, with
     * the two steps reversed, an order exists against stock that was never taken. The listener
     * must let it out so {@code BEFORE_COMMIT} can roll the whole thing back.
     */
    @Test
    @DisplayName("a failure propagates, so the BEFORE_COMMIT phase can roll the order back with it")
    void aFailurePropagates() {
        doThrow(new IllegalStateException("cart service exploded"))
                .when(cartService).removeItems(eq("user-1"), isNull(), anyMap());

        assertThatThrownBy(() -> listener.on(placed(2)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("cart service exploded");
    }
}
