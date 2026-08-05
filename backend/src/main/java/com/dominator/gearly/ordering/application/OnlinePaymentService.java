package com.dominator.gearly.ordering.application;

import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.service.user.MomoService;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * The two halves of paying for an order through an external gateway: sending the customer
 * there, and hearing back.
 *
 * <h2>Why this is a separate bean from {@link PlaceOrderService}</h2>
 * Because the outbound HTTP call must happen <em>after</em> the placement transaction commits,
 * and a class cannot call its own {@code @Transactional} method through the Spring proxy. S8
 * worked around that with an {@code ObjectProvider} self-reference and a comment saying S10
 * would remove it. This is that removal: {@link #startCheckout} is not transactional, calls a
 * different bean whose method is, and so a slow or unreachable third party can never hold a
 * database transaction open.
 *
 * <p>S13 puts the gateway behind a {@code PaymentGateway} port; until then this class names
 * {@code MomoService} directly, which is the one place in Ordering that knows a specific
 * provider exists.
 */
@Service
@RequiredArgsConstructor
public class OnlinePaymentService {

    private final PlaceOrderService placeOrderService;
    private final OrderRepository orderRepository;
    private final MomoService momoService;

    /** Places the order, then returns the URL the customer is redirected to in order to pay. */
    public Checkout startCheckout(UserId userId, PlaceOrderCommand command) {
        Order order = placeOrderService.place(userId, command);

        // The gateway client is generic and still speaks BigDecimal; the S13 port takes Money.
        BigDecimal amountUsd = order.getTotalAmount().amount();
        String paymentUrl = momoService.createPaymentUrl(amountUsd, order.getId());

        return new Checkout(order.getId(), paymentUrl);
    }

    /**
     * The gateway's IPN callback.
     *
     * <p>Recording the transaction and moving the status are one operation on the aggregate,
     * so this path can no longer land an order in a status the transition table would refuse.
     * A failed checkout returning the order to {@code PENDING} is a declared edge of that
     * table rather than the unchecked assignment it used to be.
     */
    @Transactional
    public void recordGatewayResult(String gatewayOrderId,
                                    String gatewayTransactionId,
                                    int resultCode,
                                    String rawResponse) {
        String ourOrderId = gatewayOrderId.replaceFirst("^Gearly-", "");
        Order order = orderRepository.findById(OrderId.of(ourOrderId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.recordGatewayResult(gatewayTransactionId, resultCode == 0, rawResponse);

        orderRepository.save(order);
    }

    /** Where the customer goes next, and which order they are paying for. */
    public record Checkout(String orderId, String payUrl) {}
}
