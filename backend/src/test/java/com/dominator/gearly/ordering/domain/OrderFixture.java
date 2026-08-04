package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds {@link Order} aggregates for tests.
 *
 * <p>It exists because the aggregate has no setters any more, which is the point of S10 —
 * but a test still needs an order sitting in a particular state, and a test of the read side
 * still needs one carrying the id and timestamps Mongo assigns.
 *
 * <p>Two rules keep this from quietly becoming the setter surface it replaces:
 *
 * <ol>
 *   <li><b>State is reached through real behavior.</b> {@link Builder#at(OrderStatus)} walks
 *       the order along legal edges of the transition table rather than assigning a status,
 *       so a fixture can only ever describe an order the production code could have produced.
 *       If a status becomes unreachable, the fixtures for it fail — which is information.</li>
 *   <li><b>Reflection touches only the persistence-managed fields</b> — {@code id},
 *       {@code version} and the two audit timestamps. Those are populated by Spring Data on
 *       load and by nothing else; a test that needs them is standing in for the mapper, not
 *       reaching around an invariant. Every other field goes through the aggregate.</li>
 * </ol>
 */
public final class OrderFixture {

    /** The production numbers: 8% tax, free shipping strictly above $30, otherwise $15. */
    public static final PricingPolicy PRICING =
            new PricingPolicy(new BigDecimal("0.08"), Money.of("30.00"), Money.of("15.00"));

    private OrderFixture() {
    }

    public static OrderLine line(String productId, String title, double price, int quantity) {
        return new OrderLine(ProductId.of(productId), title, Money.of(price),
                "http://img/" + productId + ".png", Quantity.of(quantity));
    }

    public static ShippingInformation shipping() {
        return new ShippingInformation("Ada", "Lovelace", "ada@example.com", "0123456789", null);
    }

    public static Builder anOrder() {
        return new Builder();
    }

    /** The legal route from {@code PENDING} to {@code target}, as edges to walk. */
    public static List<OrderStatus> pathTo(OrderStatus target) {
        return switch (target) {
            case PENDING -> List.of();
            case PROCESSING -> List.of(OrderStatus.PROCESSING);
            case SHIPPED -> List.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED);
            case DELIVERED -> List.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
            case COMPLETED -> List.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED,
                    OrderStatus.DELIVERED, OrderStatus.COMPLETED);
            case CANCELLED -> List.of(OrderStatus.CANCELLED);
            case PENDING_REFUND -> List.of(OrderStatus.PENDING_REFUND);
            case REFUNDED -> List.of(OrderStatus.PENDING_REFUND, OrderStatus.REFUNDED);
        };
    }

    public static final class Builder {

        private UserId userId = UserId.of("u1");
        private List<OrderLine> lines = new ArrayList<>(List.of(line("p1", "Product p1", 10.00, 2)));
        private ShippingInformation shippingInformation = shipping();
        private String paymentMethod = "cod";
        private OrderStatus status = OrderStatus.PENDING;
        private final List<TransactionStatus> extraTransactions = new ArrayList<>();
        private boolean reviewed;
        private String id;
        private Long version;
        private Instant addedAt;
        private Instant modifiedAt;
        private Instant doneAt;
        private String note;

        public Builder ownedBy(String userId) {
            this.userId = UserId.of(userId);
            return this;
        }

        public Builder withLines(OrderLine... lines) {
            this.lines = new ArrayList<>(List.of(lines));
            return this;
        }

        public Builder paidWith(String method) {
            this.paymentMethod = method;
            return this;
        }

        /** Walks the order to {@code status} along legal transitions. */
        public Builder at(OrderStatus status) {
            this.status = status;
            return this;
        }

        /**
         * Appends a transaction with this status — the way a test says "this order has been
         * paid for" without reaching into the payment.
         */
        public Builder withTransaction(TransactionStatus status) {
            this.extraTransactions.add(status);
            return this;
        }

        public Builder reviewed() {
            this.reviewed = true;
            return this;
        }

        public Builder withNote(String note) {
            this.note = note;
            return this;
        }

        public Builder doneAt(Instant doneAt) {
            this.doneAt = doneAt;
            return this;
        }

        /** The fields Spring Data populates on load. Reflection is the honest tool for these. */
        public Builder persistedAs(String id, Instant addedAt, Instant modifiedAt) {
            this.id = id;
            this.version = 0L;
            this.addedAt = addedAt;
            this.modifiedAt = modifiedAt;
            return this;
        }

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Order build() {
            Order order = Order.place(userId, lines, shippingInformation, paymentMethod, PRICING);

            setPersistenceField(order, "id", id);
            setPersistenceField(order, "version", version);

            // note / reviewed / doneAt through the administrative-replace path rather than
            // through reflection — they are ordinary state, and this is how production sets them
            if (note != null || doneAt != null || reviewed) {
                order.replaceContent(userId, lines, shippingInformation, order.getPayment(),
                        null, reviewed, note, doneAt, PRICING);
            }

            for (TransactionStatus transactionStatus : extraTransactions) {
                order.recordPayment(transactionStatus, "fixture");
            }
            for (OrderStatus step : pathTo(status)) {
                order.transitionTo(step);
            }

            // last, so a transition's touch() cannot overwrite what the test asked for
            setPersistenceField(order, "addedAt", addedAt);
            setPersistenceField(order, "modifiedAt", modifiedAt);
            return order;
        }

        private void setPersistenceField(Order order, String field, Object value) {
            if (value != null) {
                ReflectionTestUtils.setField(order, field, value);
            }
        }
    }
}
