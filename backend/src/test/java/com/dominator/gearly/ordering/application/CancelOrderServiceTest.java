package com.dominator.gearly.ordering.application;

import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderCannotBeCancelledException;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderNotYoursException;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.PaymentTransaction;
import com.dominator.gearly.ordering.domain.TransactionStatus;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <b>Characterization suite (S8), carried forward</b> — the cancel half of what was
 * {@code CustomerOrderServiceTest}. See {@link PlaceOrderServiceTest} on the split.
 */
@ExtendWith(MockitoExtension.class)
class CancelOrderServiceTest {

    @Mock private OrderRepository orderRepository;

    private CancelOrderService service;

    private static final UserId USER_ID = UserId.of("user-1");

    @BeforeEach
    void setUp() {
        service = new CancelOrderService(orderRepository);
    }

    /**
     * An order at {@code status} carrying the opening pending charge every placed order has,
     * plus whatever extra transactions the test asks for.
     *
     * <p>The opening charge is the one difference from the S8 fixture, which built the
     * transaction list by hand and could therefore describe a paid order that had never been
     * charged. Lines of $10 x 2 price to the same $36.60 total the old fixture assigned
     * directly, so every money assertion below is unchanged.
     */
    private Order existingOrder(OrderStatus status, TransactionStatus... txStatuses) {
        OrderFixture.Builder builder = OrderFixture.anOrder()
                .withId("order-1")
                .ownedBy(USER_ID.value())
                .withLines(OrderFixture.line("p1", "Product p1", 10.00, 2))
                .paidWith("momo");
        for (TransactionStatus txStatus : txStatuses) {
            if (txStatus != TransactionStatus.PENDING) {   // the opening charge is already PENDING
                builder.withTransaction(txStatus);
            }
        }
        return builder.at(status).build();
    }

    private void findsOrder(Order order) {
        when(orderRepository.findById(OrderId.of("order-1"))).thenReturn(Optional.of(order));
    }

    private CancelOrderCommand cancelRequest() {
        return new CancelOrderCommand("order-1", "Changed my mind");
    }

    @Test
    @DisplayName("an unpaid order is cancelled outright and the reason is stored as the note")
    void unpaid_isCancelled() {
        Order order = existingOrder(OrderStatus.PENDING, TransactionStatus.PENDING);
        findsOrder(order);

        service.cancel(USER_ID, cancelRequest());

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getNote()).isEqualTo("Changed my mind");
        assertThat(order.getPayment().getTransactions()).hasSize(1);
        verify(orderRepository).save(order);
    }

    @Test
    @DisplayName("a paid order gets a PENDING_REFUND transaction for the full total and moves to PENDING_REFUND")
    void paid_initiatesRefund() {
        // This used to contradict AdminOrderService.ALLOWED_SOURCES, which permitted
        // PENDING_REFUND only from DELIVERED. S10 reconciled the two in favour of this path —
        // the table now lists PENDING and PROCESSING as sources too, so the behavior pinned
        // here is legal from every write path rather than only this one. See
        // OrderStatusTest.pendingRefundIsReachableFromEveryPaidState.
        Order order = existingOrder(OrderStatus.PENDING, TransactionStatus.SUCCESSFUL);
        findsOrder(order);

        service.cancel(USER_ID, cancelRequest());

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING_REFUND);
        // opening pending charge, the successful payment, then the refund
        assertThat(order.getPayment().getTransactions()).hasSize(3);
        PaymentTransaction refund = order.getPayment().getTransactions().getLast();
        assertThat(refund.getStatus()).isEqualTo(TransactionStatus.PENDING_REFUND);
        assertThat(refund.getAmount()).isEqualTo(Money.of(36.60));
        assertThat(refund.getRawResponse()).isEqualTo("Refund initiated for order: order-1");
    }

    @Test
    @DisplayName("a PROCESSING order is cancellable too")
    void processing_isCancellable() {
        Order order = existingOrder(OrderStatus.PROCESSING, TransactionStatus.PENDING);
        findsOrder(order);

        service.cancel(USER_ID, cancelRequest());

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("cancelling someone else's order is forbidden")
    void otherUsersOrder_isForbidden() {
        Order order = existingOrder(OrderStatus.PENDING, TransactionStatus.PENDING);
        findsOrder(order);

        // S12: the refusal is a domain type mapped to 403 centrally, not an ApiException
        // carrying the status itself. Same status, same message — see OrderNotYoursException.
        assertThatThrownBy(() -> service.cancel(UserId.of("someone-else"), cancelRequest()))
                .isInstanceOf(OrderNotYoursException.class)
                .hasMessage("You are not allowed to cancel this order");

        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("an order past PROCESSING can no longer be cancelled")
    void shippedOrder_conflicts() {
        Order order = existingOrder(OrderStatus.SHIPPED, TransactionStatus.SUCCESSFUL);
        findsOrder(order);

        // Was ConflictException. The rule moved onto the aggregate, which may not name a web
        // type, so it throws a DomainConflictException subclass instead — mapped to the same
        // 409 by GlobalExceptionHandler, which GlobalExceptionHandlerTest pins.
        assertThatThrownBy(() -> service.cancel(USER_ID, cancelRequest()))
                .isInstanceOf(OrderCannotBeCancelledException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void missingOrder_is404() {
        when(orderRepository.findById(OrderId.of("order-1"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(USER_ID, cancelRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("an order counts as paid as soon as one of its transactions has succeeded")
    void anOrderIsPaidOnceATransactionSucceeds() {
        // This is the condition the cancel path branches on. It was an inline
        // transactions.stream().anyMatch(...) in the service, next to a public
        // initiateRefund(order, payment) that any caller could use to append a refund to any
        // payment. Both are Payment's now, and the free-floating refund helper is gone.
        Order unpaid = OrderFixture.anOrder().build();
        Order paid = OrderFixture.anOrder().withTransaction(TransactionStatus.SUCCESSFUL).build();

        assertThat(unpaid.isPaid()).isFalse();
        assertThat(paid.isPaid()).isTrue();
        assertThat(paid.getPayment().isSettled()).isTrue();
    }
}
