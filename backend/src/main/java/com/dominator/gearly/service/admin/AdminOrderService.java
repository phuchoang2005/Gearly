package com.dominator.gearly.service.admin;

import com.dominator.gearly.dto.OrderPatchDTO;
import com.dominator.gearly.dto.OrderUpsertRequestDTO;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.ordering.domain.IllegalOrderTransitionException;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.PricingPolicy;
import com.dominator.gearly.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Admin order CRUD and the status-transition workflow. Sales analytics live in
 * {@link OrderAnalyticsService}.
 *
 * <p>What used to be here and is not any more: the transition table (now on
 * {@code OrderStatus}), the money-affecting transaction effects (now on {@code Order}), the
 * field-by-field payload copy through the entity's setters (now {@code Order.replaceContent}
 * / {@code Order.amend}), and the by-hand total recompute (now {@code PricingPolicy}, via the
 * aggregate). Every write below hands the payload to the aggregate and saves what comes back.
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
    public Order updateOrder(String id, OrderUpsertRequestDTO dto) {
        Order existingOrder = findOrThrow(id);
        existingOrder.replaceContent(
                dto.getUserId(),
                dto.getItems(),
                dto.getShippingInformation(),
                dto.getPayment(),
                dto.getOrderStatus(),
                dto.isReviewed(),
                dto.getNote(),
                dto.getDoneAt(),
                pricingPolicy);
        return orderRepository.save(existingOrder);
    }

    public Order createOrder(OrderUpsertRequestDTO dto) {
        Order order = Order.createByAdministrator(
                dto.getUserId(),
                dto.getItems(),
                dto.getShippingInformation(),
                dto.getPayment(),
                dto.isReviewed(),
                dto.getNote(),
                dto.getDoneAt(),
                pricingPolicy);
        return orderRepository.save(order);
    }

    /** {@code PATCH} — correct individual fields. An absent field is left alone. */
    public Order patchOrder(String id, OrderPatchDTO dto) {
        Order existing = findOrThrow(id);
        existing.amend(
                dto.getItems(),
                dto.getShippingInformation(),
                dto.getPayment(),
                dto.getOrderStatus(),
                dto.getDoneAt(),
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
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}
