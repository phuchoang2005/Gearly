package com.dominator.gearly.ordering.application;

import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.ordering.domain.IllegalOrderTransitionException;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.PricingPolicy;
import com.dominator.gearly.shared.domain.OrderId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin order CRUD and the status-transition workflow.
 *
 * <p>What used to be here and is not any more: the transition table (now on
 * {@code OrderStatus}), the money-affecting transaction effects (now on {@code Order}), the
 * field-by-field payload copy through the entity's setters (now {@code Order.replaceContent}
 * / {@code Order.amend}), and the by-hand total recompute (now {@code PricingPolicy}, via the
 * aggregate). Every write below hands a command to the aggregate and saves what comes back.
 */
@RequiredArgsConstructor
@Service
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final PricingPolicy pricingPolicy;

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(String id) {
        return findOrThrow(id);
    }

    /** {@code PUT} — replace the whole order. */
    @Transactional
    public Order replaceOrder(String id, AdminOrderCommand command) {
        Order existingOrder = findOrThrow(id);
        existingOrder.replaceContent(
                command.userId(),
                command.lines(),
                command.shippingInformation(),
                command.payment(),
                command.orderStatus(),
                command.reviewed(),
                command.note(),
                command.doneAt(),
                pricingPolicy);
        return orderRepository.save(existingOrder);
    }

    @Transactional
    public Order createOrder(AdminOrderCommand command) {
        Order order = Order.createByAdministrator(
                command.userId(),
                command.lines(),
                command.shippingInformation(),
                command.payment(),
                command.reviewed(),
                command.note(),
                command.doneAt(),
                pricingPolicy);
        return orderRepository.save(order);
    }

    /** {@code PATCH} — correct individual fields. An absent field is left alone. */
    @Transactional
    public Order patchOrder(String id, AdminOrderPatchCommand command) {
        Order existing = findOrThrow(id);
        existing.amend(
                command.lines(),
                command.shippingInformation(),
                command.payment(),
                command.orderStatus(),
                command.doneAt(),
                pricingPolicy);
        return orderRepository.save(existing);
    }

    /**
     * Move an order to {@code target} when its current status permits it, recording a payment
     * transaction for money-affecting transitions.
     *
     * <p>Returns {@code false} rather than propagating the 409 the aggregate throws. This is
     * the one place in the system where a refused transition is not a 409: the seven
     * {@code /api/admin/orders/{id}/set-*} endpoints have always answered {@code 200 false}
     * and the admin frontend reads that boolean, so the contract is preserved here rather
     * than turned into a frontend change.
     */
    @Transactional
    public boolean transition(String id, OrderStatus target) {
        Order order = findOrThrow(id);
        try {
            order.transitionTo(target);
        } catch (IllegalOrderTransitionException refused) {
            return false;
        }
        orderRepository.save(order);
        return true;
    }

    private Order findOrThrow(String id) {
        return orderRepository.findById(OrderId.of(id))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}
