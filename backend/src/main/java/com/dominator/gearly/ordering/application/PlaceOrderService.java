package com.dominator.gearly.ordering.application;

import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderPlaced;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.PricingPolicy;
import com.dominator.gearly.service.user.ProductService;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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
 * <p>This service no longer calls Catalog or Cart. It publishes {@link OrderPlaced} and
 * {@code OrderPlacedListener} reacts {@code BEFORE_COMMIT} — same two writes, same order, same
 * transaction, but placement stops naming two other contexts to get them.
 */
@Service
@RequiredArgsConstructor
public class PlaceOrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final PricingPolicy pricingPolicy;
    private final ApplicationEventPublisher events;

    @Transactional
    public Order place(UserId userId, PlaceOrderCommand command) {
        List<OrderLine> lines = snapshotFromCatalog(command.lines());

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
     * Turns catalog ids and quantities into order lines, copying the title, price and image as
     * they are right now.
     *
     * <p>This is the seam S11 replaces with the {@code CatalogSnapshot} anti-corruption layer,
     * at which point Ordering stops naming {@code Product} at all. The stock check here is one
     * of the five duplicated copies S11 collapses onto {@code Product.reserve(Quantity)}; it
     * stays as-is so this sprint changes no placement behavior.
     */
    private List<OrderLine> snapshotFromCatalog(List<PlaceOrderCommand.RequestedLine> requested) {
        List<OrderLine> lines = new ArrayList<>();
        for (PlaceOrderCommand.RequestedLine line : requested) {
            Product product = productService.getProductById(line.productId());
            if (product.getStock() < line.quantity()) {
                throw new BadRequestException("Insufficient stock for product: " + product.getTitle());
            }
            lines.add(new OrderLine(
                    ProductId.of(product.getId()),
                    product.getTitle(),
                    product.getPrice(),
                    product.getImages().getFirst().getUrl(),
                    Quantity.of(line.quantity())));
        }
        return lines;
    }
}
