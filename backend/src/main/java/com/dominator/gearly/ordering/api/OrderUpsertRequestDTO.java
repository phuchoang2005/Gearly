package com.dominator.gearly.ordering.api;

import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.Payment;
import com.dominator.gearly.ordering.domain.ShippingInformation;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.UserId;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

/**
 * Admin create/update payload for an order. Replaces binding the raw {@link
 * com.dominator.gearly.ordering.domain.Order} aggregate directly to the request
 * body: the managed fields (id and the audit timestamps) are intentionally
 * absent so a client can never set them.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderUpsertRequestDTO {
    private UserId userId;
    private List<OrderLine> items;

    /**
     * Accepted and ignored.
     *
     * <p>An order's total is derived from its lines by {@code PricingPolicy}, so this field
     * can no longer set it — sending lines worth $10 alongside a total of $10,000 used to
     * store both, and nothing in the system noticed. The property stays on the DTO because
     * the admin frontend sends the whole order back on save, and dropping it would turn
     * every one of those calls into a 400 on an unrecognized field.
     */
    private Money totalAmount;

    private Payment payment;
    private OrderStatus orderStatus;
    private ShippingInformation shippingInformation;
    private boolean reviewed;
    private String note;
    private Instant doneAt;
}
