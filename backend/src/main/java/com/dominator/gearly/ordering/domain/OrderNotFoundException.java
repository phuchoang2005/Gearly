package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.DomainNotFoundException;

/**
 * No such order. Answers 404, as the {@code ResourceNotFoundException} it replaces did.
 *
 * <p>The message is deliberately the bare "Order not found" all four call sites used — the id is
 * not echoed back, because {@code GET /api/orders/{id}} answers 403 for somebody else's order
 * (S12) and a 404 that quoted the id would be a slightly better oracle than one that does not.
 */
public class OrderNotFoundException extends DomainNotFoundException {

    public OrderNotFoundException() {
        super("Order not found");
    }
}
