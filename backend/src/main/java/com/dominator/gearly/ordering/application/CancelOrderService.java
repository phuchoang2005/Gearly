package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.OrderNotFoundException;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderNotYoursException;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A customer cancelling one of their own orders.
 *
 * <p>Almost nothing happens here, which is the point. Whether the order has gone too far to
 * cancel, whether the money has arrived and a refund is therefore owed, which status that
 * lands on and what gets written to the payment ledger are all {@code Order.cancel}'s. This
 * class loads the aggregate, checks that the caller owns it, and saves.
 *
 * <p>Ownership is checked here rather than in the aggregate because it decides an HTTP 403,
 * and the domain may not name a status code. {@code order.isOwnedBy(userId)} is the aggregate
 * answering the question; turning "no" into a 403 is this layer's job. S12 replaced the
 * {@code ApiException(HttpStatus.FORBIDDEN, …)} with {@link OrderNotYoursException}, mapped
 * centrally, the same shape the {@code DomainConflictException} mapping already has — so this
 * class no longer names {@code org.springframework.http} and the refusal reads the same way
 * here as it does on the read path {@code OrderQueryService.findById} guards. The message is
 * unchanged.
 */
@Service
@RequiredArgsConstructor
public class CancelOrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public void cancel(UserId userId, CancelOrderCommand command) {
        Order order = orderRepository.findById(OrderId.of(command.orderId()))
                .orElseThrow(() -> new OrderNotFoundException());

        if (!order.isOwnedBy(userId)) {
            throw OrderNotYoursException.toCancel();
        }

        order.cancel(command.reason());
        orderRepository.save(order);
    }
}
