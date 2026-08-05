package com.dominator.gearly.ordering.application;

import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.catalog.domain.InsufficientStockException;
import com.dominator.gearly.catalog.domain.ProductSnapshotPort;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.OrderPlaced;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.Payment;
import com.dominator.gearly.ordering.domain.PaymentTransaction;
import com.dominator.gearly.ordering.domain.PricingPolicy;
import com.dominator.gearly.ordering.domain.ShippingInformation;
import com.dominator.gearly.ordering.domain.TransactionStatus;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
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
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <b>Characterization suite (S8), carried forward.</b> These assertions were
 * {@code CustomerOrderServiceTest}'s and are unchanged in substance: the 240-line service they
 * pinned was split in S10 into {@link PlaceOrderService}, {@link CancelOrderService},
 * {@link OrderQueryService} and {@link OnlinePaymentService}, so its suite is split the same
 * way. The behavior each one locks down is what makes the split provably safe.
 *
 * <p>This half covers placement: pricing, the catalog snapshot, the opening payment, and the
 * stock and cart side effects.
 */
@ExtendWith(MockitoExtension.class)
class PlaceOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductSnapshotPort catalog;
    @Mock private ApplicationEventPublisher events;

    private PlaceOrderService service;

    private static final UserId USER_ID = UserId.of("user-1");

    /** The production numbers: 8% tax, free shipping strictly above $30, otherwise $15. */
    private final PricingPolicy pricingPolicy = OrderFixture.PRICING;

    @BeforeEach
    void setUp() {
        service = new PlaceOrderService(orderRepository, catalog, pricingPolicy, events);
    }

    // ---- fixtures ----------------------------------------------------------

    /**
     * What the catalog publishes about a product. Placement sees this and nothing else now —
     * the {@code ProductService} that used to be mocked here handed over a whole
     * {@code Product}, which is the coupling the ACL removes.
     */
    private CatalogSnapshot snapshot(String id, double price, int stock) {
        return new CatalogSnapshot(ProductId.of(id), "Product " + id, "Author " + id,
                Money.of(price), "http://img/" + id + ".png", ProductCondition.NEW,
                Quantity.of(stock));
    }

    private void catalogHas(String id, double price, int stock) {
        when(catalog.snapshotOf(ProductId.of(id))).thenReturn(snapshot(id, price, stock));
    }

    private PlaceOrderCommand order(String productId, int quantity) {
        return new PlaceOrderCommand(
                List.of(new PlaceOrderCommand.RequestedLine(productId, quantity)),
                "cod",
                new ShippingInformation("Ada", "Lovelace", "ada@example.com", "0123456789", null));
    }

    private Order captureSavedOrder() {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        return captor.getValue();
    }

    private void stubSaveReturnsArgument() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- pricing -----------------------------------------------------------

    @Nested
    @DisplayName("pricing")
    class Pricing {

        @Test
        @DisplayName("tax is 8% of the item subtotal, shipping is $15 below the threshold")
        void belowThreshold_charges15Shipping_and8PercentTax() {
            catalogHas("p1", 10.00, 100);
            stubSaveReturnsArgument();

            service.place(USER_ID, order("p1", 2));

            // subtotal 20.00 + tax 1.60 + shipping 15.00
            assertThat(captureSavedOrder().getTotalAmount()).isEqualTo(Money.of(36.60));
        }

        @Test
        @DisplayName("a subtotal above $30 ships free")
        void aboveThreshold_shipsFree() {
            catalogHas("p1", 40.00, 100);
            stubSaveReturnsArgument();

            service.place(USER_ID, order("p1", 1));

            // subtotal 40.00 + tax 3.20 + shipping 0.00
            assertThat(captureSavedOrder().getTotalAmount()).isEqualTo(Money.of(43.20));
        }

        @Test
        @DisplayName("a subtotal of exactly $30 still pays shipping — the threshold is strictly greater-than")
        void exactlyAtThreshold_stillCharges15Shipping() {
            catalogHas("p1", 30.00, 100);
            stubSaveReturnsArgument();

            service.place(USER_ID, order("p1", 1));

            // subtotal 30.00 + tax 2.40 + shipping 15.00 — NOT free at the boundary
            assertThat(captureSavedOrder().getTotalAmount()).isEqualTo(Money.of(47.40));
        }

        @Test
        @DisplayName("tax rounds HALF_UP to two decimals")
        void taxRoundsHalfUpToCents() {
            catalogHas("p1", 3.19, 100);
            stubSaveReturnsArgument();

            service.place(USER_ID, order("p1", 1));

            // 3.19 * 0.08 = 0.2552 -> 0.26; + 15.00 shipping = 18.45
            assertThat(captureSavedOrder().getTotalAmount()).isEqualTo(Money.of(18.45));
        }
    }

    // ---- the rest of placement --------------------------------------------

    @Test
    @DisplayName("placement copies title, price and image from the catalog's snapshot")
    void snapshotsCatalogFields() {
        catalogHas("p1", 12.50, 100);
        stubSaveReturnsArgument();

        service.place(USER_ID, order("p1", 3));

        Order saved = captureSavedOrder();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getItems()).singleElement().satisfies(line -> {
            assertThat(line.getProductId().value()).isEqualTo("p1");
            assertThat(line.getTitle()).isEqualTo("Product p1");
            assertThat(line.getPrice()).isEqualTo(Money.of(12.50));
            assertThat(line.getQuantity().toInt()).isEqualTo(3);
            assertThat(line.getImageUrl()).isEqualTo("http://img/p1.png");
        });
    }

    @Test
    @DisplayName("placement opens the payment with a single PENDING transaction for the grand total")
    void buildsInitialPendingPayment() {
        catalogHas("p1", 10.00, 100);
        stubSaveReturnsArgument();

        service.place(USER_ID, order("p1", 2));

        Payment payment = captureSavedOrder().getPayment();
        assertThat(payment.getMethod()).isEqualTo("cod");
        assertThat(payment.getTransactions()).singleElement().satisfies(tx -> {
            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(tx.getAmount()).isEqualTo(Money.of(36.60));
            assertThat(tx.getRawResponse()).isEqualTo("Pending payment: 36.6");
            assertThat(tx.getTransactionId()).isNotBlank();
            assertThat(tx.getCreatedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("FIXED (was a KNOWN BUG): a freshly placed order accepts further transactions in memory")
    void paymentAcceptsFurtherTransactions() {
        // The S8 suite pinned this as a bug and said S10 must build a mutable list.
        // buildInitialPayment used List.of(...), so appending to a just-placed order threw
        // UnsupportedOperationException — cancel and the gateway callback only appeared to
        // work because a round trip through Mongo turned the list into an ArrayList.
        //
        // Payment owns the list now and it is always mutable. What is handed out is still an
        // unmodifiable view, but deliberately so: appending goes through the aggregate, which
        // is what stops the ledger gaining a row nothing decided to add.
        catalogHas("p1", 10.00, 100);
        stubSaveReturnsArgument();

        Order order = service.place(USER_ID, order("p1", 2));

        order.recordPayment(TransactionStatus.SUCCESSFUL, "settled");
        assertThat(order.getPayment().getTransactions()).hasSize(2);

        assertThatThrownBy(() -> order.getPayment().getTransactions()
                .add(new PaymentTransaction("t", TransactionStatus.PENDING, Money.ZERO, null, null)))
                .as("the exposed list is a read-only view — append through the aggregate")
                .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * The stock decrement is {@code CatalogStockListener}'s and the cart clear is
     * {@code CartOrderListener}'s — each context reacting for itself. What this asserts is the
     * half that belongs to placement: that the announcement carries everything a listener
     * needs, in types neither listener has to reach into ordering to understand.
     */
    @Test
    @DisplayName("placement announces OrderPlaced with the buyer, the quantities and the total")
    void publishesOrderPlaced() {
        catalogHas("p1", 10.00, 100);
        stubSaveReturnsArgument();

        service.place(USER_ID, order("p1", 2));

        ArgumentCaptor<OrderPlaced> captor = ArgumentCaptor.forClass(OrderPlaced.class);
        verify(events).publishEvent(captor.capture());
        OrderPlaced event = captor.getValue();

        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.totalAmount()).isEqualTo(Money.of(36.60));
        assertThat(event.quantities())
                .containsExactly(entry(ProductId.of("p1"), Quantity.of(2)));
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    @DisplayName("placement rejects an order for more units than are in stock, before saving anything")
    void insufficientStock_throwsAndSavesNothing() {
        catalogHas("p1", 10.00, 1);

        assertThatThrownBy(() -> service.place(USER_ID, order("p1", 2)))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Only 1 left for \"Product p1\"!");

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    @DisplayName("placement allows ordering the exact remaining stock")
    void exactStock_isAllowed() {
        catalogHas("p1", 10.00, 2);
        stubSaveReturnsArgument();

        service.place(USER_ID, order("p1", 2));

        verify(events).publishEvent(any(OrderPlaced.class));
    }

    @Test
    @DisplayName("an order line keeps its per-line quantity when the same product appears once")
    void orderLinesCarryQuantities() {
        // The listener used to build its map with Collectors.toMap, which threw on a duplicate
        // productId. OrderPlaced merges repeated lines now, so this is no longer a landmine.
        catalogHas("p1", 10.00, 100);
        stubSaveReturnsArgument();

        Order placed = service.place(USER_ID, order("p1", 4));

        assertThat(placed.getItems()).extracting(line -> line.getQuantity().toInt())
                .containsExactly(4);
    }

    @Test
    @DisplayName("an order line knows what it costs, so no caller has to compute a subtotal")
    void aLineKnowsItsOwnTotal() {
        OrderLine line = OrderFixture.line("p1", "GPU", 10.50, 3);

        assertThat(line.lineTotal()).isEqualTo(Money.of(31.50));
    }
}
