package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.CancelOrderRequestDTO;
import com.dominator.gearly.dto.CreateOrderResponse;
import com.dominator.gearly.dto.OrderCreationRequestDTO;
import com.dominator.gearly.dto.OrderItemRequestDTO;
import com.dominator.gearly.dto.PaymentRequestDTO;
import com.dominator.gearly.exception.ApiException;
import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ConflictException;
import com.dominator.gearly.mapper.OrderMapper;
import com.dominator.gearly.model.Image;
import com.dominator.gearly.model.Order;
import com.dominator.gearly.model.OrderItem;
import com.dominator.gearly.model.OrderStatus;
import com.dominator.gearly.model.Payment;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.ShippingInformation;
import com.dominator.gearly.model.Transaction;
import com.dominator.gearly.model.TransactionStatus;
import com.dominator.gearly.model.User;
import com.dominator.gearly.repository.OrderRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <b>Characterization suite (S8).</b> Locks the <i>current</i> behavior of
 * {@link CustomerOrderService} — including its known bugs — so that the S10 rewrite of
 * this service into an {@code ordering} aggregate can be proven behavior-preserving.
 *
 * <p>Where a test pins something the DDD plan calls out as wrong, the test name and its
 * comment say so explicitly. Those are the only assertions a later sprint may edit, and
 * only in the same commit that makes the deliberate change.
 *
 * <p>The real {@link OrderMapper} is used rather than a mock: the price/title snapshot it
 * builds is part of the behavior under characterization.
 */
@ExtendWith(MockitoExtension.class)
class CustomerOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductService productService;
    @Mock private CartService cartService;
    @Mock private MomoService momoService;

    private CustomerOrderService service;

    private static final String USER_ID = "user-1";

    /**
     * Stands in for the Spring proxy {@code createOrderAndGetMomoUrl} routes through.
     * Holding it here lets a test assert that the proxy was used rather than {@code this}
     * — which is exactly the self-invocation bug S8 fixed.
     */
    private ObjectProvider<CustomerOrderService> selfProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        selfProvider = mock(ObjectProvider.class);
        service = new CustomerOrderService(orderRepository, productService, cartService,
                momoService, new OrderMapper(), selfProvider);
        lenient().when(selfProvider.getObject()).thenReturn(service);
    }

    // ---- fixtures ----------------------------------------------------------

    private AuthenticatedUser authUser(String userId) {
        User user = new User();
        user.setId(userId);
        return new AuthenticatedUser(user);
    }

    private Product product(String id, double price, int stock) {
        Product p = new Product();
        p.setId(id);
        p.setTitle("Product " + id);
        p.setPrice(price);
        p.setStock(stock);
        p.setImages(List.of(new Image("http://img/" + id + ".png", "alt")));
        return p;
    }

    private OrderCreationRequestDTO orderRequest(String productId, int quantity) {
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);

        PaymentRequestDTO payment = new PaymentRequestDTO();
        payment.setMethod("cod");

        OrderCreationRequestDTO dto = new OrderCreationRequestDTO();
        dto.setItems(List.of(item));
        dto.setPaymentInfo(payment);
        dto.setShippingInformation(new ShippingInformation(
                "Ada", "Lovelace", "ada@example.com", "0123456789", null));
        return dto;
    }

    /** Captures the Order handed to {@code orderRepository.save}. */
    private Order captureSavedOrder() {
        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        return captor.getValue();
    }

    private void stubSaveReturnsArgument() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- createOrder: money ------------------------------------------------

    @Nested
    @DisplayName("createOrder — pricing")
    class Pricing {

        @Test
        @DisplayName("tax is 8% of the item subtotal, shipping is $15 below the threshold")
        void belowThreshold_charges15Shipping_and8PercentTax() {
            when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 100));
            stubSaveReturnsArgument();

            service.createOrder(authUser(USER_ID), orderRequest("p1", 2));

            // subtotal 20.00 + tax 1.60 + shipping 15.00
            assertThat(captureSavedOrder().getTotalAmount()).isEqualTo(36.60);
        }

        @Test
        @DisplayName("a subtotal above $30 ships free")
        void aboveThreshold_shipsFree() {
            when(productService.getProductById("p1")).thenReturn(product("p1", 40.00, 100));
            stubSaveReturnsArgument();

            service.createOrder(authUser(USER_ID), orderRequest("p1", 1));

            // subtotal 40.00 + tax 3.20 + shipping 0.00
            assertThat(captureSavedOrder().getTotalAmount()).isEqualTo(43.20);
        }

        @Test
        @DisplayName("a subtotal of exactly $30 still pays shipping — the threshold is strictly greater-than")
        void exactlyAtThreshold_stillCharges15Shipping() {
            when(productService.getProductById("p1")).thenReturn(product("p1", 30.00, 100));
            stubSaveReturnsArgument();

            service.createOrder(authUser(USER_ID), orderRequest("p1", 1));

            // subtotal 30.00 + tax 2.40 + shipping 15.00 — NOT free at the boundary
            assertThat(captureSavedOrder().getTotalAmount()).isEqualTo(47.40);
        }

        @Test
        @DisplayName("tax rounds HALF_UP to two decimals")
        void taxRoundsHalfUpToCents() {
            when(productService.getProductById("p1")).thenReturn(product("p1", 3.19, 100));
            stubSaveReturnsArgument();

            service.createOrder(authUser(USER_ID), orderRequest("p1", 1));

            // 3.19 * 0.08 = 0.2552 -> 0.26; + 15.00 shipping = 18.45
            assertThat(captureSavedOrder().getTotalAmount()).isEqualTo(18.45);
        }
    }

    // ---- createOrder: the rest --------------------------------------------

    @Test
    @DisplayName("createOrder snapshots title, price and the first image off the catalog product")
    void createOrder_snapshotsCatalogFields() {
        when(productService.getProductById("p1")).thenReturn(product("p1", 12.50, 100));
        stubSaveReturnsArgument();

        service.createOrder(authUser(USER_ID), orderRequest("p1", 3));

        Order saved = captureSavedOrder();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getProductId()).isEqualTo("p1");
            assertThat(item.getTitle()).isEqualTo("Product p1");
            assertThat(item.getPrice()).isEqualTo(12.50);
            assertThat(item.getQuantity()).isEqualTo(3);
            assertThat(item.getImageUrl()).isEqualTo("http://img/p1.png");
        });
    }

    @Test
    @DisplayName("createOrder opens the payment with a single PENDING transaction for the grand total")
    void createOrder_buildsInitialPendingPayment() {
        when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 100));
        stubSaveReturnsArgument();

        service.createOrder(authUser(USER_ID), orderRequest("p1", 2));

        Payment payment = captureSavedOrder().getPayment();
        assertThat(payment.getMethod()).isEqualTo("cod");
        assertThat(payment.getTransactions()).singleElement().satisfies(tx -> {
            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
            assertThat(tx.getAmount()).isEqualTo(36.60);
            assertThat(tx.getRawResponse()).isEqualTo("Pending payment: 36.6");
            assertThat(tx.getTransactionId()).isNotBlank();
            assertThat(tx.getCreatedAt()).isNotNull();
        });
    }

    @Test
    @DisplayName("KNOWN BUG: the freshly built transaction list is immutable, so appending to it in-memory throws")
    void createOrder_transactionListIsImmutable() {
        // buildInitialPayment uses List.of(...). Only the Mongo round-trip turns this into a
        // mutable ArrayList, which is why cancelOrder/updateOrderStatusFromMomo appear to work
        // in production but not on a just-created in-memory order. S10 must build a mutable list.
        when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 100));
        stubSaveReturnsArgument();

        Order order = service.createOrder(authUser(USER_ID), orderRequest("p1", 2));

        assertThatThrownBy(() -> order.getPayment().getTransactions().add(new Transaction()))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("createOrder decrements stock and removes the ordered quantities from the cart")
    void createOrder_appliesStockAndClearsCart() {
        when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 100));
        stubSaveReturnsArgument();

        service.createOrder(authUser(USER_ID), orderRequest("p1", 2));

        verify(productService).decreaseStock("p1", 2);
        verify(cartService).removeItems(eq(USER_ID), isNull(), eq(Map.of("p1", 2)));
    }

    @Test
    @DisplayName("createOrder rejects an order for more units than are in stock, before saving anything")
    void createOrder_insufficientStock_throwsAndSavesNothing() {
        when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 1));

        assertThatThrownBy(() -> service.createOrder(authUser(USER_ID), orderRequest("p1", 2)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Insufficient stock for product: Product p1");

        verify(orderRepository, never()).save(any());
        verifyNoInteractions(cartService);
    }

    @Test
    @DisplayName("createOrder allows ordering the exact remaining stock")
    void createOrder_exactStock_isAllowed() {
        when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 2));
        stubSaveReturnsArgument();

        service.createOrder(authUser(USER_ID), orderRequest("p1", 2));

        verify(productService).decreaseStock("p1", 2);
    }

    // ---- createOrderAndGetMomoUrl -----------------------------------------

    @Test
    @DisplayName("createOrderAndGetMomoUrl returns the new order id and the gateway URL for its total")
    void createOrderAndGetMomoUrl_returnsIdAndPayUrl() {
        when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 100));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("order-9");
            return o;
        });
        when(momoService.createPaymentUrl(new BigDecimal("36.6"), "order-9"))
                .thenReturn("https://momo.test/pay/order-9");

        CreateOrderResponse response =
                service.createOrderAndGetMomoUrl(authUser(USER_ID), orderRequest("p1", 2));

        assertThat(response.getOrderId()).isEqualTo("order-9");
        assertThat(response.getPayUrl()).isEqualTo("https://momo.test/pay/order-9");
    }

    @Test
    @DisplayName("createOrderAndGetMomoUrl places the order through the proxy, not through 'this'")
    void createOrderAndGetMomoUrl_routesPlacementThroughTheProxy() {
        // Regression guard for the S8 self-invocation fix. A direct this.createOrder(...)
        // call would skip the @Transactional advice, so order placement would run with no
        // transaction at all now that a transaction manager actually exists.
        when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 100));
        stubSaveReturnsArgument();

        service.createOrderAndGetMomoUrl(authUser(USER_ID), orderRequest("p1", 2));

        verify(selfProvider).getObject();
    }

    // ---- cancelOrder -------------------------------------------------------

    @Nested
    @DisplayName("cancelOrder")
    class CancelOrder {

        private Order existingOrder(OrderStatus status, TransactionStatus... txStatuses) {
            Order order = new Order();
            order.setId("order-1");
            order.setUserId(USER_ID);
            order.setOrderStatus(status);
            order.setTotalAmount(36.60);

            List<Transaction> transactions = new ArrayList<>();
            for (TransactionStatus txStatus : txStatuses) {
                Transaction tx = new Transaction();
                tx.setStatus(txStatus);
                tx.setCreatedAt(Instant.now());
                transactions.add(tx);
            }
            Payment payment = new Payment();
            payment.setMethod("momo");
            payment.setTransactions(transactions);
            order.setPayment(payment);
            return order;
        }

        private CancelOrderRequestDTO cancelRequest() {
            return new CancelOrderRequestDTO("order-1", "Changed my mind");
        }

        @Test
        @DisplayName("an unpaid order is cancelled outright and the reason is stored as the note")
        void unpaid_isCancelled() {
            Order order = existingOrder(OrderStatus.PENDING, TransactionStatus.PENDING);
            when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

            service.cancelOrder(authUser(USER_ID), cancelRequest());

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
            assertThat(order.getNote()).isEqualTo("Changed my mind");
            assertThat(order.getPayment().getTransactions()).hasSize(1);
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("a paid order gets a PENDING_REFUND transaction for the full total and moves to PENDING_REFUND")
        void paid_initiatesRefund() {
            // NOTE: this contradicts AdminOrderService.ALLOWED_SOURCES, which permits
            // PENDING_REFUND only from DELIVERED. S10 reconciles the two in favour of this path.
            Order order = existingOrder(OrderStatus.PENDING, TransactionStatus.SUCCESSFUL);
            when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

            service.cancelOrder(authUser(USER_ID), cancelRequest());

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_REFUND);
            assertThat(order.getPayment().getTransactions()).hasSize(2);
            Transaction refund = order.getPayment().getTransactions().get(1);
            assertThat(refund.getStatus()).isEqualTo(TransactionStatus.PENDING_REFUND);
            assertThat(refund.getAmount()).isEqualTo(36.60);
            assertThat(refund.getRawResponse()).isEqualTo("Refund initiated for order: order-1");
        }

        @Test
        @DisplayName("a PROCESSING order is cancellable too")
        void processing_isCancellable() {
            Order order = existingOrder(OrderStatus.PROCESSING, TransactionStatus.PENDING);
            when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

            service.cancelOrder(authUser(USER_ID), cancelRequest());

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("cancelling someone else's order is forbidden")
        void otherUsersOrder_isForbidden() {
            Order order = existingOrder(OrderStatus.PENDING, TransactionStatus.PENDING);
            when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.cancelOrder(authUser("someone-else"), cancelRequest()))
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("an order past PROCESSING can no longer be cancelled")
        void shippedOrder_conflicts() {
            Order order = existingOrder(OrderStatus.SHIPPED, TransactionStatus.SUCCESSFUL);
            when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> service.cancelOrder(authUser(USER_ID), cancelRequest()))
                    .isInstanceOf(ConflictException.class);

            verify(orderRepository, never()).save(any());
        }
    }

    // ---- updateOrderStatusFromMomo ----------------------------------------

    @Nested
    @DisplayName("updateOrderStatusFromMomo")
    class MomoCallback {

        private Order pendingOrder() {
            Order order = new Order();
            order.setId("order-1");
            order.setUserId(USER_ID);
            order.setOrderStatus(OrderStatus.PENDING);
            order.setTotalAmount(36.60);

            Payment payment = new Payment();
            payment.setMethod("momo");
            payment.setTransactions(new ArrayList<>());
            order.setPayment(payment);
            return order;
        }

        @Test
        @DisplayName("resultCode 0 records a SUCCESSFUL transaction and moves the order to PROCESSING")
        void success_movesToProcessing() {
            Order order = pendingOrder();
            when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

            service.updateOrderStatusFromMomo("Gearly-order-1", "momo-tx-1", 0, "{\"ok\":true}");

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(order.getPayment().getTransactions()).singleElement().satisfies(tx -> {
                assertThat(tx.getTransactionId()).isEqualTo("momo-tx-1");
                assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);
                assertThat(tx.getAmount()).isEqualTo(36.60);
                assertThat(tx.getRawResponse()).isEqualTo("{\"ok\":true}");
            });
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("a non-zero resultCode records a FAILED transaction and leaves the order PENDING")
        void failure_staysPending() {
            Order order = pendingOrder();
            order.setOrderStatus(OrderStatus.PROCESSING);
            when(orderRepository.findById("order-1")).thenReturn(Optional.of(order));

            service.updateOrderStatusFromMomo("Gearly-order-1", "momo-tx-2", 1006, "{\"ok\":false}");

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getPayment().getTransactions()).singleElement()
                    .extracting(Transaction::getStatus)
                    .isEqualTo(TransactionStatus.FAILED);
        }

        @Test
        @DisplayName("only a leading Gearly- prefix is stripped from the gateway's order id")
        void stripsOnlyTheLeadingPrefix() {
            Order order = pendingOrder();
            when(orderRepository.findById("order-Gearly-1")).thenReturn(Optional.of(order));

            service.updateOrderStatusFromMomo("Gearly-order-Gearly-1", "momo-tx-3", 0, "{}");

            verify(orderRepository).save(order);
        }
    }

    // ---- initiateRefund (called directly by the cancel path) ---------------

    @Test
    @DisplayName("initiateRefund appends a PENDING_REFUND transaction for the order total")
    void initiateRefund_appendsPendingRefundTransaction() {
        Order order = new Order();
        order.setId("order-7");
        order.setTotalAmount(99.99);
        Payment payment = new Payment();
        payment.setTransactions(new ArrayList<>());

        service.initiateRefund(order, payment);

        assertThat(payment.getTransactions()).singleElement().satisfies(tx -> {
            assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING_REFUND);
            assertThat(tx.getAmount()).isEqualTo(99.99);
            assertThat(tx.getRawResponse()).isEqualTo("Refund initiated for order: order-7");
            assertThat(tx.getTransactionId()).isNotBlank();
        });
    }

    // ---- read paths --------------------------------------------------------

    @Test
    @DisplayName("getOrderCountsByStatus reports every status plus a totalInProgress roll-up")
    void getOrderCountsByStatus_includesEveryStatusAndTotalInProgress() {
        for (OrderStatus status : OrderStatus.values()) {
            when(orderRepository.countByUserIdAndOrderStatus(USER_ID, status)).thenReturn(1L);
        }
        when(orderRepository.countByUserIdAndOrderStatusNotIn(eq(USER_ID), any())).thenReturn(5L);

        Map<String, Long> counts = service.getOrderCountsByStatus(authUser(USER_ID));

        assertThat(counts).hasSize(OrderStatus.values().length + 1);
        assertThat(counts).containsEntry("PENDING", 1L).containsEntry("totalInProgress", 5L);
    }

    @Test
    @DisplayName("an OrderItem list keeps its per-line quantity when the same product appears once")
    void orderItemsCarryQuantities() {
        // Guards the Collectors.toMap in applyStockAndClearCart: it would throw on a duplicate
        // productId, which is why the request DTO is expected to pre-merge lines.
        when(productService.getProductById("p1")).thenReturn(product("p1", 10.00, 100));
        stubSaveReturnsArgument();

        Order order = service.createOrder(authUser(USER_ID), orderRequest("p1", 4));

        assertThat(order.getItems()).extracting(OrderItem::getQuantity).containsExactly(4);
    }
}
