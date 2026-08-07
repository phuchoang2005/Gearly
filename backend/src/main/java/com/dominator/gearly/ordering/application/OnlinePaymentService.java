package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.OrderNotFoundException;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.payments.domain.GatewaySettlement;
import com.dominator.gearly.payments.domain.PaymentGateway;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * <h2>S13: the provider is behind a port</h2>
 * This class named {@code MomoService} directly and knew three MoMo facts along with it — that
 * the gateway speaks {@code BigDecimal}, that it prefixes our order ids with {@code Gearly-},
 * and that {@code resultCode == 0} means the money moved. All three are the adapter's now. What
 * is left here is the ordering use case: place, then send the customer to pay; hear back, then
 * record it on the aggregate.
 */
@Service
@RequiredArgsConstructor
public class OnlinePaymentService {

    private final PlaceOrderService placeOrderService;
    private final OrderRepository orderRepository;
    private final PaymentGateway paymentGateway;

    /** Places the order, then returns the URL the customer is redirected to in order to pay. */
    public Checkout startCheckout(UserId userId, PlaceOrderCommand command) {
        Order order = placeOrderService.place(userId, command);

        String paymentUrl = paymentGateway.startCheckout(order.getTotalAmount(), order.getId());

        return new Checkout(order.getId(), paymentUrl);
    }

    /**
     * Applies a settlement the gateway has already been shown to have signed.
     *
     * <p>Recording the transaction and moving the status are one operation on the aggregate,
     * so this path can no longer land an order in a status the transition table would refuse.
     * A failed checkout returning the order to {@code PENDING} is a declared edge of that
     * table rather than the unchecked assignment it used to be.
     *
     * <p>Takes a {@link GatewaySettlement} rather than the gateway's own fields: authentication
     * and translation happen at the adapter, so by the time this runs there is nothing left to
     * decide about whether the notification is genuine.
     */
    @Transactional
    public void recordSettlement(GatewaySettlement settlement) {
        Order order = orderRepository.findById(OrderId.of(settlement.orderReference()))
                .orElseThrow(() -> new OrderNotFoundException());

        order.recordGatewayResult(
                settlement.transactionId(), settlement.successful(), settlement.rawNotification());

        orderRepository.save(order);
    }

    /** Where the customer goes next, and which order they are paying for. */
    public record Checkout(String orderId, String payUrl) {}
}
