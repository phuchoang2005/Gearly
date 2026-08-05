package com.dominator.gearly.ordering.application;

import com.dominator.gearly.catalog.domain.ProductSnapshotPort;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderPlaced;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.PricingPolicy;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Placing an order: the one use case at the centre of the system.
 *
 * <p>Takes a {@link UserId} and a {@link PlaceOrderCommand}, never an
 * {@code AuthenticatedUser} — the controller unwraps the principal, so a Spring Security type
 * never reaches a use case and this class is constructible in a test with no security context.
 *
 * <h2>The transaction, and what happens inside it</h2>
 * {@code @Transactional} belongs here and only here: one aggregate, one transaction. The
 * gateway redirect that used to follow placement lives in {@link OnlinePaymentService} now, a
 * different bean, which is what finally removes the {@code ObjectProvider} self-reference S8
 * had to introduce.
 *
 * <p>This service no longer calls Catalog or Cart. It publishes {@link OrderPlaced} and the
 * two contexts that care react {@code BEFORE_COMMIT} — same writes, same order, same
 * transaction, but placement stops naming two other contexts to get them.
 *
 * <h2>What S11 changed here</h2>
 * The private {@code snapshotFromCatalog} that used to sit at the bottom of this class is
 * gone. It held a {@code ProductService}, read five fields off a {@code Product}, and made its
 * own stock check — one of the five copies of that rule. Ordering asks the catalog for a
 * {@code CatalogSnapshot} through {@link ProductSnapshotPort} now and
 * {@link OrderLine#fromSnapshot} does the rest, so this class no longer names {@code Product}
 * at all and the stock rule it used to restate lives on the aggregate that owns it.
 */
@Service
@RequiredArgsConstructor
public class PlaceOrderService {

    private final OrderRepository orderRepository;
    private final ProductSnapshotPort catalog;
    private final PricingPolicy pricingPolicy;
    private final ApplicationEventPublisher events;

    @Transactional
    public Order place(UserId userId, PlaceOrderCommand command) {
        List<OrderLine> lines = command.lines().stream()
                .map(this::toLine)
                .toList();

        Order order = Order.place(
                userId,
                lines,
                command.shippingInformation(),
                command.paymentMethod(),
                pricingPolicy);
        Order placed = orderRepository.save(order);

        // Published from here rather than recorded on the aggregate, because an order has no
        // identity until the insert returns — see OrderPlaced on why that matters and what it
        // would take to change.
        events.publishEvent(OrderPlaced.from(placed));
        return placed;
    }

    /**
     * One requested line, priced and titled as the catalog has it right now.
     *
     * @throws com.dominator.gearly.catalog.domain.ProductNotFoundException if it has been
     *         delisted since the customer put it in their basket — a 404 where the previous
     *         code dereferenced a {@code null} and produced a 500
     */
    private OrderLine toLine(PlaceOrderCommand.RequestedLine line) {
        return OrderLine.fromSnapshot(
                catalog.snapshotOf(ProductId.of(line.productId())),
                Quantity.of(line.quantity()));
    }
}
