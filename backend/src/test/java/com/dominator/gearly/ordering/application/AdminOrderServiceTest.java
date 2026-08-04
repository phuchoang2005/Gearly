package com.dominator.gearly.ordering.application;

import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.ordering.domain.IllegalOrderTransitionException;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.PaymentTransaction;
import com.dominator.gearly.ordering.domain.PricingPolicy;
import com.dominator.gearly.ordering.domain.ShippingInformation;
import com.dominator.gearly.ordering.domain.TransactionStatus;
import com.dominator.gearly.ordering.domain.OrderRepository;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The admin write paths, now that all of them go through the aggregate.
 *
 * <p>{@code PaymentFactory} is no longer a mock — it was a {@code @Service} that did nothing
 * but build domain objects, and is now a static factory inside {@code ordering.domain}. The
 * assertions moved with it, from "the service called the collaborator" to "the order ended up
 * in this state", which is what the endpoints actually promise.
 */
@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock private OrderRepository orderRepository;

    private AdminOrderService service;

    /** The production numbers: 8% tax, free shipping above $30, otherwise $15. */
    private final PricingPolicy pricingPolicy =
            new PricingPolicy(new BigDecimal("0.08"), Money.of("30.00"), Money.of("15.00"));

    @BeforeEach
    void setUp() {
        service = new AdminOrderService(orderRepository, pricingPolicy);
    }

    // ---- fixtures ----------------------------------------------------------

    private OrderLine line(String productId, double price, int quantity) {
        return new OrderLine(ProductId.of(productId), "Product " + productId,
                Money.of(price), "http://img/" + productId + ".png", Quantity.of(quantity));
    }

    private AdminOrderCommand upsert(OrderStatus status, List<OrderLine> lines) {
        return new AdminOrderCommand(UserId.of("u1"), lines,
                new ShippingInformation("Ada", "Lovelace", "ada@example.com", "0123456789", null),
                null, status, false, null, null);
    }

    /** An order sitting at {@code status} — reached by transitioning to it, never by assignment. */
    private Order orderWithStatus(OrderStatus status) {
        return OrderFixture.anOrder()
                .ownedBy("u1")
                .withLines(OrderFixture.line("p1", "Product p1", 10.00, 2))
                .at(status)
                .build();
    }

    private void findsOrder(Order order) {
        when(orderRepository.findById(OrderId.of("o1"))).thenReturn(Optional.of(order));
    }

    private void saveReturnsArgument() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ---- transition --------------------------------------------------------

    @Nested
    @DisplayName("transition")
    class Transition {

        @Test
        void allowedSource_movesStatusAndSaves() {
            Order order = orderWithStatus(OrderStatus.PENDING);
            findsOrder(order);

            boolean ok = service.transition("o1", OrderStatus.PROCESSING);

            assertThat(ok).isTrue();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            verify(orderRepository).save(order);
            // PROCESSING is not a money-affecting transition — only the opening charge exists
            assertThat(order.getPayment().getTransactions()).hasSize(1);
        }

        @Test
        @DisplayName("a refused transition answers false and saves nothing — the 200-false contract")
        void disallowedSource_returnsFalseAndDoesNotSave() {
            Order order = orderWithStatus(OrderStatus.PENDING);
            findsOrder(order);

            boolean ok = service.transition("o1", OrderStatus.SHIPPED); // only PROCESSING -> SHIPPED

            assertThat(ok).isFalse();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            verify(orderRepository, never()).save(any());
        }

        @Test
        void toDelivered_recordsSuccessfulTransaction() {
            Order order = orderWithStatus(OrderStatus.SHIPPED);
            findsOrder(order);

            boolean ok = service.transition("o1", OrderStatus.DELIVERED);

            assertThat(ok).isTrue();
            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
            assertThat(order.getPayment().getTransactions())
                    .last()
                    .satisfies(tx -> {
                        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESSFUL);
                        assertThat(tx.getRawResponse()).isEqualTo("Payment successful");
                        assertThat(tx.getAmount()).isEqualTo(order.getTotalAmount());
                    });
            verify(orderRepository).save(order);
        }

        @Test
        void toRefunded_recordsRefundTransaction() {
            Order order = orderWithStatus(OrderStatus.PENDING_REFUND);
            findsOrder(order);

            service.transition("o1", OrderStatus.REFUNDED);

            assertThat(order.getPayment().getTransactions())
                    .last()
                    .extracting(PaymentTransaction::getStatus)
                    .isEqualTo(TransactionStatus.REFUNDED);
        }

        @Test
        void orderNotFound_throws() {
            when(orderRepository.findById(OrderId.of("missing"))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.transition("missing", OrderStatus.PROCESSING))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ---- createOrder -------------------------------------------------------

    @Nested
    @DisplayName("createOrder")
    class Create {

        @Test
        @DisplayName("opens PENDING with a pending charge, whatever status the payload asked for")
        void opensPendingWithACharge() {
            saveReturnsArgument();
            Order order = service.createOrder(upsert(OrderStatus.DELIVERED, List.of(line("p1", 10.00, 2))));

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            assertThat(order.getPayment().getMethod()).isEqualTo("cod");
            assertThat(order.getPayment().getTransactions()).singleElement()
                    .extracting(PaymentTransaction::getStatus)
                    .isEqualTo(TransactionStatus.PENDING);
        }

        /**
         * The command has no totalAmount component at all, which is the structural half of
         * this fix: a payload total worth $10,000 against lines worth $10 used to store both,
         * and now stops at the api layer where the DTO documents the field as ignored.
         */
        @Test
        @DisplayName("the total is priced from the lines — the command cannot carry one")
        void pricesTheOrderFromItsLines() {
            saveReturnsArgument();

            Order order = service.createOrder(upsert(null, List.of(line("p1", 10.00, 2))));

            // subtotal 20.00 + tax 1.60 + shipping 15.00
            assertThat(order.getTotalAmount()).isEqualTo(Money.of(36.60));
        }
    }

    // ---- updateOrder (PUT) -------------------------------------------------

    @Nested
    @DisplayName("updateOrder")
    class Update {

        @Test
        void copiesTheAdminSettableFields() {
            Order order = orderWithStatus(OrderStatus.PENDING);
            findsOrder(order);
            saveReturnsArgument();

            AdminOrderCommand command = new AdminOrderCommand(UserId.of("u1"),
                    List.of(line("p2", 40.00, 1)),
                    new ShippingInformation("Ada", "Lovelace", "ada@example.com", "0123456789", null),
                    null, OrderStatus.PROCESSING, true, "gift wrap", null);

            Order updated = service.replaceOrder("o1", command);

            assertThat(updated.getUserId()).isEqualTo(UserId.of("u1"));
            assertThat(updated.getItems()).singleElement()
                    .extracting(l -> l.getProductId().value()).isEqualTo("p2");
            assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
            assertThat(updated.isReviewed()).isTrue();
            assertThat(updated.getNote()).isEqualTo("gift wrap");
            // subtotal 40.00 + tax 3.20 + free shipping
            assertThat(updated.getTotalAmount()).isEqualTo(Money.of(43.20));
        }

        @Test
        @DisplayName("a status the order cannot reach is a 409, not a silent assignment")
        void anIllegalStatusInThePayloadIsRefused() {
            Order order = orderWithStatus(OrderStatus.PENDING);
            findsOrder(order);

            assertThatThrownBy(() ->
                    service.replaceOrder("o1", upsert(OrderStatus.REFUNDED, List.of(line("p1", 10.00, 1)))))
                    .isInstanceOf(IllegalOrderTransitionException.class);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("re-sending the status the order already has is a no-op, not a refusal")
        void anUnchangedStatusIsNotATransition() {
            Order order = orderWithStatus(OrderStatus.PENDING);
            findsOrder(order);
            saveReturnsArgument();

            Order updated = service.replaceOrder("o1", upsert(OrderStatus.PENDING, List.of(line("p1", 10.00, 1))));

            assertThat(updated.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        }
    }

    // ---- patchOrder --------------------------------------------------------

    @Nested
    @DisplayName("patchOrder")
    class Patch {

        @Test
        @DisplayName("an absent field is left alone")
        void onlyTouchesWhatIsPresent() {
            Order order = orderWithStatus(OrderStatus.PENDING);
            findsOrder(order);
            saveReturnsArgument();
            Money before = order.getTotalAmount();

            Order patched = service.patchOrder("o1", new AdminOrderPatchCommand(null,
                    new ShippingInformation("Grace", "Hopper", "grace@example.com", "0999", null),
                    null, null, null));

            assertThat(patched.getShippingInformation().getFirstName()).isEqualTo("Grace");
            assertThat(patched.getTotalAmount()).isEqualTo(before);
            assertThat(patched.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("patching the lines re-prices the order through the same rule placement uses")
        void patchingLinesRepricesTheOrder() {
            Order order = orderWithStatus(OrderStatus.PENDING);
            findsOrder(order);
            saveReturnsArgument();

            Order patched = service.patchOrder("o1", new AdminOrderPatchCommand(
                    List.of(line("p1", 10.00, 3)), null, null, null, null));

            // subtotal 30.00 + tax 2.40 + shipping 15.00 — the threshold is strictly greater-than
            assertThat(patched.getTotalAmount()).isEqualTo(Money.of(47.40));
        }

        /**
         * The bypass this sprint exists to close. {@code PATCH {"orderStatus":"REFUNDED"}} on a
         * {@code PENDING} order used to assign the status straight from the request body,
         * skipping the transition table entirely.
         */
        @Test
        @DisplayName("PATCH can no longer set a status the transition table refuses")
        void patchingAnIllegalStatusIsRefused() {
            Order order = orderWithStatus(OrderStatus.PENDING);
            findsOrder(order);

            AdminOrderPatchCommand command =
                    new AdminOrderPatchCommand(null, null, null, OrderStatus.REFUNDED, null);

            assertThatThrownBy(() -> service.patchOrder("o1", command))
                    .isInstanceOf(IllegalOrderTransitionException.class);

            assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("a legal status change through PATCH records its transaction effect too")
        void patchingALegalStatusAppliesTheTransition() {
            Order order = orderWithStatus(OrderStatus.SHIPPED);
            findsOrder(order);
            saveReturnsArgument();

            Order patched = service.patchOrder("o1",
                    new AdminOrderPatchCommand(null, null, null, OrderStatus.DELIVERED, null));

            assertThat(patched.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
            assertThat(patched.getPayment().getTransactions())
                    .last()
                    .extracting(PaymentTransaction::getStatus)
                    .isEqualTo(TransactionStatus.SUCCESSFUL);
        }
    }
}
