package com.dominator.gearly.model;

import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.shared.domain.Money;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "orders")
public class Order {
    @Id
    private String id;

    /**
     * Optimistic-locking token. Stops two concurrent writers — say an admin status
     * transition and a customer cancellation, or a MoMo callback arriving while an admin
     * is editing — from silently overwriting each other's version of the order.
     *
     * <p>{@code @JsonIgnore}: internal, never on the wire, never client-settable.
     * Boxed, and backfilled to 0 by {@code data/seed/migrate.js} — see {@code Product}.
     */
    @Version
    @JsonIgnore
    private Long version;

    private String userId;
    @Indexed(name = "idx_items_productId")
    private List<OrderItem> items;

    /** Defaults to zero so an order document without the field reads as it always did. */
    private Money totalAmount = Money.ZERO;
    private Payment payment;
    private OrderStatus orderStatus;
    private ShippingInformation shippingInformation;
    private boolean isReviewed;
    private String note;
    @CreatedDate
    private Instant addedAt;
    @LastModifiedDate
    private Instant modifiedAt;
    private Instant doneAt;
}
