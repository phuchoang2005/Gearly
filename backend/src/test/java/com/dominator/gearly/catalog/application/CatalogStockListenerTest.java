package com.dominator.gearly.catalog.application;

import com.dominator.gearly.catalog.domain.InsufficientStockException;
import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductFixture;
import com.dominator.gearly.catalog.domain.ProductNotFoundException;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.ordering.domain.OrderCancelled;
import com.dominator.gearly.ordering.domain.OrderPlaced;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The catalog's half of what {@code OrderPlacedListenerTest} covered: the stock decrement.
 * The other half — the cart clear — is {@code CartOrderListenerTest}'s.
 *
 * <p>The restock assertions have no predecessor. Nothing gave cancelled units back before this
 * sprint, which S10 identified and deliberately deferred; these are the tests for the fix.
 */
@ExtendWith(MockitoExtension.class)
class CatalogStockListenerTest {

    @Mock private ProductRepository products;

    private CatalogStockListener listener;

    private static final UserId BUYER = UserId.of("user-1");

    @BeforeEach
    void setUp() {
        listener = new CatalogStockListener(products);
    }

    private OrderPlaced placed(Map<ProductId, Quantity> quantities) {
        return new OrderPlaced(BUYER, quantities, Money.of(100.0), Instant.now());
    }

    private OrderCancelled cancelled(Map<ProductId, Quantity> quantities) {
        return new OrderCancelled(OrderId.of("o1"), BUYER, quantities,
                OrderStatus.CANCELLED, false, "changed my mind", Instant.now());
    }

    @SuppressWarnings("unchecked")
    private List<Product> captureSaved() {
        ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
        verify(products).saveAll(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("OrderPlaced — units come off the shelf")
    class Placement {

        @Test
        @DisplayName("each line's quantity is reserved against its product")
        void reservesEachLine() {
            Product gpu = ProductFixture.product("p1", 10.0, 10);
            Product cpu = ProductFixture.product("p2", 20.0, 4);
            when(products.findById(ProductId.of("p1"))).thenReturn(Optional.of(gpu));
            when(products.findById(ProductId.of("p2"))).thenReturn(Optional.of(cpu));

            listener.on(placed(Map.of(
                    ProductId.of("p1"), Quantity.of(3),
                    ProductId.of("p2"), Quantity.of(1))));

            assertThat(gpu.getStock()).isEqualTo(Quantity.of(7));
            assertThat(cpu.getStock()).isEqualTo(Quantity.of(3));
            assertThat(captureSaved()).containsExactlyInAnyOrder(gpu, cpu);
        }

        @Test
        @DisplayName("an oversell is refused and nothing is written")
        void oversellIsRefusedAndNothingIsSaved() {
            when(products.findById(ProductId.of("p1")))
                    .thenReturn(Optional.of(ProductFixture.product("p1", 10.0, 1)));

            assertThatThrownBy(() -> listener.on(placed(Map.of(ProductId.of("p1"), Quantity.of(2)))))
                    .isInstanceOf(InsufficientStockException.class);

            // The listener runs BEFORE_COMMIT, so throwing is what rolls the order back with it.
            verify(products, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("a product that no longer exists fails the placement rather than silently passing")
        void missingProductFailsPlacement() {
            when(products.findById(ProductId.of("gone"))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> listener.on(placed(Map.of(ProductId.of("gone"), Quantity.ONE))))
                    .isInstanceOf(ProductNotFoundException.class);

            verify(products, never()).saveAll(anyList());
        }
    }

    @Nested
    @DisplayName("OrderCancelled — units go back on it")
    class Cancellation {

        @Test
        @DisplayName("cancelling restocks exactly what the order took")
        void restocksTheCancelledUnits() {
            Product gpu = ProductFixture.product("p1", 10.0, 7);
            when(products.findById(ProductId.of("p1"))).thenReturn(Optional.of(gpu));

            listener.on(cancelled(Map.of(ProductId.of("p1"), Quantity.of(3))));

            assertThat(gpu.getStock()).isEqualTo(Quantity.of(10));
            assertThat(captureSaved()).containsExactly(gpu);
        }

        @Test
        @DisplayName("a product deleted since the order was placed is skipped, not fatal")
        void missingProductDoesNotBlockTheCancellation() {
            // A customer's cancellation must not be held hostage to a catalog row that no
            // longer exists — there is nothing to put the units back on.
            when(products.findById(ProductId.of("gone"))).thenReturn(Optional.empty());

            listener.on(cancelled(Map.of(ProductId.of("gone"), Quantity.of(2))));

            assertThat(captureSaved()).isEmpty();
        }

        @Test
        @DisplayName("place then cancel leaves the catalog exactly where it started")
        void placeThenCancelIsANoOp() {
            Product gpu = ProductFixture.product("p1", 10.0, 5);
            when(products.findById(ProductId.of("p1"))).thenReturn(Optional.of(gpu));

            listener.on(placed(Map.of(ProductId.of("p1"), Quantity.of(2))));
            listener.on(cancelled(Map.of(ProductId.of("p1"), Quantity.of(2))));

            assertThat(gpu.getStock()).isEqualTo(Quantity.of(5));
        }
    }
}
