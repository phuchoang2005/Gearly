package com.dominator.gearly.service.admin;

import com.dominator.gearly.dto.OrderPatchDTO;
import com.dominator.gearly.dto.OrderUpsertRequestDTO;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.mapper.OrderMapper;
import com.dominator.gearly.model.Order;
import com.dominator.gearly.model.OrderStatus;
import com.dominator.gearly.model.TransactionStatus;
import com.dominator.gearly.repository.OrderRepository;
import com.dominator.gearly.service.common.PaymentFactory;
import com.dominator.gearly.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin order CRUD and the status-transition workflow. Sales analytics live in
 * {@link OrderAnalyticsService}; payment/transaction assembly in
 * {@link PaymentFactory}.
 */
@RequiredArgsConstructor
@Service
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final PaymentFactory paymentFactory;
    private final OrderMapper orderMapper;

    /** Source statuses from which each target status may be reached. */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_SOURCES = Map.of(
            OrderStatus.PROCESSING,     EnumSet.of(OrderStatus.PENDING),
            OrderStatus.SHIPPED,        EnumSet.of(OrderStatus.PROCESSING),
            OrderStatus.DELIVERED,      EnumSet.of(OrderStatus.SHIPPED),
            OrderStatus.COMPLETED,      EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.CANCELLED,      EnumSet.of(OrderStatus.PENDING, OrderStatus.PROCESSING),
            OrderStatus.PENDING_REFUND, EnumSet.of(OrderStatus.DELIVERED),
            OrderStatus.REFUNDED,       EnumSet.of(OrderStatus.PENDING_REFUND)
    );

    /** Transitions that also record a payment transaction on the order. */
    private static final Map<OrderStatus, TxEffect> TX_EFFECTS = Map.of(
            OrderStatus.DELIVERED,      new TxEffect(TransactionStatus.SUCCESSFUL, "Payment successful"),
            OrderStatus.PENDING_REFUND, new TxEffect(TransactionStatus.PENDING_REFUND, "Pending refund..."),
            OrderStatus.REFUNDED,       new TxEffect(TransactionStatus.REFUNDED, "Refund...")
    );

    private record TxEffect(TransactionStatus status, String rawResponse) {}

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(String id) {
        return findOrThrow(id);
    }

    public Order updateOrder(String id, OrderUpsertRequestDTO dto) {
        Order existingOrder = findOrThrow(id);
        orderMapper.applyUpsert(existingOrder, dto);
        existingOrder.setModifiedAt(Instant.now());
        return orderRepository.save(existingOrder);
    }

    public Order createOrder(OrderUpsertRequestDTO dto) {
        Order order = new Order();
        orderMapper.applyUpsert(order, dto);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setAddedAt(Instant.now());
        order.setModifiedAt(Instant.now());
        paymentFactory.appendTransaction(order, TransactionStatus.PENDING, null);
        return orderRepository.save(order);
    }

    public Order patchOrder(String id, OrderPatchDTO dto) {
        Order existing = findOrThrow(id);

        if (dto.getOrderStatus() != null) {
            existing.setOrderStatus(dto.getOrderStatus());
        }
        if (dto.getShippingInformation() != null) {
            existing.setShippingInformation(dto.getShippingInformation());
        }
        if (dto.getPayment() != null) {
            existing.setPayment(dto.getPayment());
        }
        if (dto.getItems() != null) {
            existing.setItems(dto.getItems());
            Money total = existing.getItems().stream()
                    .map(i -> i.getPrice().times(i.getQuantity()))
                    .reduce(Money.ZERO, Money::plus);
            existing.setTotalAmount(total);
        }
        if (dto.getDoneAt() != null) {
            existing.setDoneAt(dto.getDoneAt());
        }

        existing.setModifiedAt(Instant.now());
        return orderRepository.save(existing);
    }

    /**
     * Move an order to {@code target} when its current status permits it, recording a
     * payment transaction for money-affecting transitions. Returns {@code false} when
     * the transition is not allowed from the order's current status.
     */
    @Transactional
    public boolean transition(String id, OrderStatus target) {
        Order order = findOrThrow(id);

        Set<OrderStatus> allowedSources = ALLOWED_SOURCES.get(target);
        if (allowedSources == null || !allowedSources.contains(order.getOrderStatus())) {
            return false;
        }

        order.setOrderStatus(target);
        order.setModifiedAt(Instant.now());

        TxEffect effect = TX_EFFECTS.get(target);
        if (effect != null) {
            paymentFactory.appendTransaction(order, effect.status(), effect.rawResponse());
        }

        orderRepository.save(order);
        return true;
    }

    private Order findOrThrow(String id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}
