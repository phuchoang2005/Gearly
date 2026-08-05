package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.ShippingInformation;

import java.util.List;

/**
 * A customer's intent to place an order: what they want, where it goes, how they will pay.
 *
 * <p>Deliberately <b>not</b> the request DTO. The DTO is the wire shape, with its Jakarta
 * validation annotations and whatever fields the frontends happen to send; this is the use
 * case's input. Keeping them separate is what lets the api layer change — a field renamed, a
 * new frontend, a different validation message — without the application layer noticing, and
 * it is why a use-case test needs no HTTP types to construct one.
 *
 * <p>The buyer is not on it. Identity comes from the authenticated principal as a separate
 * {@code UserId} argument, so there is no field a request body could set to place an order in
 * someone else's name.
 *
 * @param lines what to buy, by catalog id and quantity — never with a price, which the
 *              catalog supplies (S11 hardens the equivalent path on the cart, where a
 *              client-supplied price is currently persisted)
 */
public record PlaceOrderCommand(List<RequestedLine> lines,
                                String paymentMethod,
                                ShippingInformation shippingInformation) {

    public PlaceOrderCommand {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    public record RequestedLine(String productId, int quantity) {}
}
