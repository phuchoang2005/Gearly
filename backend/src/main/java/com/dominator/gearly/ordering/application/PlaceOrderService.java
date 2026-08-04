package com.dominator.gearly.ordering.application;

import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.PricingPolicy;
import com.dominator.gearly.service.user.CartService;
import com.dominator.gearly.service.user.ProductService;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Placing an order: the one use case at the centre of the system.
 *
 * <p>Takes a {@link UserId} and a {@link PlaceOrderCommand}, never an
 * {@code AuthenticatedUser} — the controller unwraps the principal, so a Spring Security type
 * never reaches a use case and this class is constructible in a test with no security context.
 *
 * <h2>The transaction</h2>
 * {@code @Transactional} belongs here and only here: one aggregate, one transaction. The
 * gateway redirect that used to follow the placement lives in {@link OnlinePaymentService}
 * now, a different bean, which is what finally removes the {@code ObjectProvider}
 * self-reference S8 had to introduce. There is no self-call left to route through a proxy.
 */
@Service
@RequiredArgsConstructor
public class PlaceOrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CartService cartService;
    private final PricingPolicy pricingPolicy;

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

        applyStockAndClearCart(userId, lines);
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

    /**
     * Both of these cross an aggregate boundary from inside the placement transaction, which
     * the working agreements say should be a domain event. That is the next commit; keeping
     * the direct calls here first means the move to events is a change to <em>how</em> they
     * are triggered and not to what happens.
     */
    private void applyStockAndClearCart(UserId userId, List<OrderLine> lines) {
        for (OrderLine line : lines) {
            productService.decreaseStock(line.getProductId().value(), line.getQuantity().toInt());
        }
        Map<String, Integer> quantitiesByProduct = lines.stream()
                .collect(Collectors.toMap(
                        line -> line.getProductId().value(),
                        line -> line.getQuantity().toInt()));
        cartService.removeItems(userId.value(), null, quantitiesByProduct);
    }
}
