package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.PaymentTransaction;
import com.dominator.gearly.ordering.domain.ShippingInformation;
import com.dominator.gearly.ordering.domain.TransactionStatus;
import com.dominator.gearly.service.user.MomoService;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <b>Characterization suite (S8), carried forward</b> — the gateway half of what was
 * {@code CustomerOrderServiceTest}. See {@link PlaceOrderServiceTest} on the split.
 *
 * <p>{@link PlaceOrderService} is a mock here, and that is the structural point of this class:
 * the S8 suite had to hold an {@code ObjectProvider} and assert that placement went through
 * the Spring proxy rather than through {@code this}, because the transactional placement and
 * the non-transactional gateway call lived on the same bean. They are two beans now, so the
 * seam is gone and there is nothing left to assert about it — the collaboration is an ordinary
 * mocked dependency.
 */
@ExtendWith(MockitoExtension.class)
class OnlinePaymentServiceTest {

    @Mock private PlaceOrderService placeOrderService;
    @Mock private OrderRepository orderRepository;
    @Mock private MomoService momoService;

    private OnlinePaymentService service;

    private static final UserId USER_ID = UserId.of("user-1");

    @BeforeEach
    void setUp() {
        service = new OnlinePaymentService(placeOrderService, orderRepository, momoService);
    }

    /**
     * One instance, reused. {@code PlaceOrderCommand} is a record, but it carries a
     * {@link ShippingInformation}, which is an entity inside the order boundary and has no
     * value equality — so two structurally identical commands are not {@code equals}, and a
     * stub matched on a freshly built one would never fire.
     */
    private final PlaceOrderCommand command = new PlaceOrderCommand(
            List.of(new PlaceOrderCommand.RequestedLine("p1", 2)),
            "momo",
            new ShippingInformation("Ada", "Lovelace", "ada@example.com", "0123456789", null));

    /** A pending order totalling $36.60, as placed: $10 x 2, plus 8% tax, plus $15 shipping. */
    private Order pendingOrder(String id) {
        return OrderFixture.anOrder()
                .withId(id)
                .ownedBy(USER_ID.value())
                .withLines(OrderFixture.line("p1", "Product p1", 10.00, 2))
                .paidWith("momo")
                .build();
    }

    // ---- starting a checkout ----------------------------------------------

    @Test
    @DisplayName("startCheckout returns the new order id and the gateway URL for its total")
    void returnsIdAndPayUrl() {
        Order placed = pendingOrder(null);
        ReflectionTestUtils.setField(placed, "id", "order-9");
        when(placeOrderService.place(USER_ID, command)).thenReturn(placed);
        // Money always carries scale 2, so the gateway sees 36.60 where it once saw 36.6.
        // MomoService scales to whole VND before signing, so the amount charged is unchanged;
        // only the BigDecimal's scale, which equals() is sensitive to, differs.
        when(momoService.createPaymentUrl(new BigDecimal("36.60"), "order-9"))
                .thenReturn("https://momo.test/pay/order-9");

        OnlinePaymentService.Checkout checkout = service.startCheckout(USER_ID, command);

        assertThat(checkout.orderId()).isEqualTo("order-9");
        assertThat(checkout.payUrl()).isEqualTo("https://momo.test/pay/order-9");
    }

    @Test
    @DisplayName("the gateway is called only after placement has returned")
    void placesBeforeCallingTheGateway() {
        Order placed = pendingOrder("order-9");
        when(placeOrderService.place(USER_ID, command)).thenReturn(placed);

        service.startCheckout(USER_ID, command);

        // The transaction is PlaceOrderService's alone and has committed by the time this
        // bean regains control, so the outbound HTTP call cannot hold one open. Before S8 the
        // two were the same method; before S10 they were the same bean, reached through an
        // ObjectProvider self-reference.
        verify(placeOrderService).place(USER_ID, command);
        verify(momoService).createPaymentUrl(new BigDecimal("36.60"), "order-9");
    }

    // ---- the IPN callback --------------------------------------------------

    @Nested
    @DisplayName("recordGatewayResult")
    class GatewayCallback {

        @Test
        @DisplayName("resultCode 0 records a SUCCESSFUL transaction and moves the order to PROCESSING")
        void success_movesToProcessing() {
            Order order = pendingOrder("order-1");
            when(orderRepository.findById(OrderId.of("order-1"))).thenReturn(Optional.of(order));

            service.recordGatewayResult("Gearly-order-1", "momo-tx-1", 0, "{\"ok\":true}");

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(order.getPayment().getTransactions()).hasSize(2).last().satisfies(tx -> {
                assertThat(tx.getTransactionId()).isEqualTo("momo-tx-1");
                assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);
                assertThat(tx.getAmount()).isEqualTo(Money.of(36.60));
                assertThat(tx.getRawResponse()).isEqualTo("{\"ok\":true}");
            });
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("a non-zero resultCode records a FAILED transaction and leaves the order PENDING")
        void failure_staysPending() {
            // Starts at PROCESSING on purpose: it proves the callback *forces* PENDING rather
            // than merely leaving an order that was already there. That reversal is now a
            // declared edge of the transition table — see OrderStatusTest — instead of an
            // assignment that skipped the table entirely.
            Order order = OrderFixture.anOrder()
                    .withId("order-1")
                    .ownedBy(USER_ID.value())
                    .withLines(OrderFixture.line("p1", "Product p1", 10.00, 2))
                    .paidWith("momo")
                    .at(OrderStatus.PROCESSING)
                    .build();
            when(orderRepository.findById(OrderId.of("order-1"))).thenReturn(Optional.of(order));

            service.recordGatewayResult("Gearly-order-1", "momo-tx-2", 1006, "{\"ok\":false}");

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getPayment().getTransactions()).last()
                    .extracting(PaymentTransaction::getStatus)
                    .isEqualTo(TransactionStatus.FAILED);
        }

        @Test
        @DisplayName("only a leading Gearly- prefix is stripped from the gateway's order id")
        void stripsOnlyTheLeadingPrefix() {
            Order order = pendingOrder("order-Gearly-1");
            when(orderRepository.findById(OrderId.of("order-Gearly-1"))).thenReturn(Optional.of(order));

            service.recordGatewayResult("Gearly-order-Gearly-1", "momo-tx-3", 0, "{}");

            verify(orderRepository).save(order);
        }
    }
}
