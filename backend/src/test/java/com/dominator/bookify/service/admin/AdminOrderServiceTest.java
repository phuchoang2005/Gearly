package com.dominator.bookify.service.admin;

import com.dominator.bookify.exception.ResourceNotFoundException;
import com.dominator.bookify.model.Order;
import com.dominator.bookify.model.OrderStatus;
import com.dominator.bookify.model.TransactionStatus;
import com.dominator.bookify.repository.OrderRepository;
import com.dominator.bookify.service.common.PaymentFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentFactory paymentFactory;
    @InjectMocks private AdminOrderService service;

    private Order orderWithStatus(OrderStatus status) {
        Order o = new Order();
        o.setOrderStatus(status);
        return o;
    }

    @Test
    void transition_allowedSource_movesStatusAndSaves() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        boolean ok = service.transition("o1", OrderStatus.PROCESSING);

        assertThat(ok).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderRepository).save(order);
        verifyNoInteractions(paymentFactory); // PROCESSING is not a money-affecting transition
    }

    @Test
    void transition_disallowedSource_returnsFalseAndDoesNotSave() {
        Order order = orderWithStatus(OrderStatus.PENDING);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        boolean ok = service.transition("o1", OrderStatus.SHIPPED); // only PROCESSING -> SHIPPED

        assertThat(ok).isFalse();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void transition_toDelivered_recordsSuccessfulTransaction() {
        Order order = orderWithStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById("o1")).thenReturn(Optional.of(order));

        boolean ok = service.transition("o1", OrderStatus.DELIVERED);

        assertThat(ok).isTrue();
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
        verify(paymentFactory).appendTransaction(order, TransactionStatus.SUCCESSFUL, "Payment successful");
        verify(orderRepository).save(order);
    }

    @Test
    void transition_orderNotFound_throws() {
        when(orderRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.transition("missing", OrderStatus.PROCESSING))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
