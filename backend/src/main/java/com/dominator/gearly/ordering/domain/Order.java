package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <b>The core aggregate of the system.</b> An order and everything inside its consistency
 * boundary: its lines, its shipping details, and the payment record accumulating against it.
 *
 * <h2>What changed, and why it matters</h2>
 * This was a Lombok {@code @Getter @Setter} data bag with a fully public setter surface, and
 * the rules that were supposed to govern it lived in whichever service happened to touch it.
 * That is how the codebase ended up with four write paths for a status and a table that
 * governed one of them, and with three different answers to "what is this order's total".
 *
 * <p>There are no setters now. Every field changes through named behavior that states the
 * rule it is enforcing, so an illegal order is not something you have to remember not to
 * create — it is something you cannot express:
 *
 * <ul>
 *   <li><b>Status</b> moves only through {@link #transitionTo} / {@link #cancel}, both of
 *       which ask {@link OrderStatus}'s table first. {@code PATCH}, {@code PUT}, the admin
 *       {@code set-*} endpoints and the gateway callback all route through them.</li>
 *   <li><b>The total</b> is never assigned. It is derived by {@link PricingPolicy} whenever
 *       the lines change, so the request body cannot contradict the lines it ships with.</li>
 *   <li><b>Transactions</b> are appended only by {@link #recordPayment}, so the ledger cannot
 *       gain a row that does not correspond to something the aggregate did.</li>
 * </ul>
 *
 * <p>ArchUnit's {@code aggregates_expose_no_public_setters} is what keeps it that way: it
 * fails on any public {@code set*} method here, which also catches someone putting Lombok's
 * {@code @Setter} or {@code @Data} back on the class.
 *
 * <h2>Persistence</h2>
 * Still a {@code @Document}, deliberately: keeping the aggregate as the mapped entity is what
 * lets the stored shape stay byte-identical to what it was, so none of this needed a
 * migration. Spring Data instantiates it through the private no-arg constructor and populates
 * the fields reflectively, which is also why they are not {@code final} — {@code @Version}
 * has to be writable after a save.
 */
@Getter
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

    /** The buyer, by id only. An order never holds a {@code User}. */
    private UserId userId;

    @Indexed(name = "idx_items_productId")
    private List<OrderLine> items;

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

    /**
     * Transitions that also record a payment transaction. Was {@code TX_EFFECTS} on
     * {@code AdminOrderService}; it belongs with the status change it accompanies, not
     * beside the one caller that used to perform it.
     */
    private static final Map<OrderStatus, TxEffect> TX_EFFECTS;

    static {
        TX_EFFECTS = new EnumMap<>(OrderStatus.class);
        TX_EFFECTS.put(OrderStatus.DELIVERED, new TxEffect(TransactionStatus.SUCCESSFUL, "Payment successful"));
        TX_EFFECTS.put(OrderStatus.PENDING_REFUND, new TxEffect(TransactionStatus.PENDING_REFUND, "Pending refund..."));
        TX_EFFECTS.put(OrderStatus.REFUNDED, new TxEffect(TransactionStatus.REFUNDED, "Refund..."));
    }

    private record TxEffect(TransactionStatus status, String rawResponse) {}

    /** For Spring Data. */
    private Order() {
    }

    // ------------------------------------------------------------------------
    // Creation
    // ------------------------------------------------------------------------

    /**
     * A customer places an order. The total is priced here rather than passed in, so there is
     * no way to place an order whose total disagrees with its lines.
     */
    public static Order place(UserId userId,
                              List<OrderLine> lines,
                              ShippingInformation shippingInformation,
                              String paymentMethod,
                              PricingPolicy pricingPolicy) {
        Objects.requireNonNull(userId, "an order must have a buyer");
        Objects.requireNonNull(pricingPolicy, "an order must be priced");
        if (lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("an order must have at least one line");
        }

        Order order = new Order();
        order.userId = userId;
        order.items = new ArrayList<>(lines);
        order.shippingInformation = shippingInformation;
        order.totalAmount = pricingPolicy.totalFor(order.items);
        order.payment = PaymentFactory.newPendingPayment(paymentMethod, order.totalAmount);
        order.orderStatus = OrderStatus.PENDING;
        return order;
    }

    /**
     * An administrator creates an order by hand — a phone order, or a correction. Opens
     * {@code PENDING} with a pending cash-on-delivery charge, exactly as
     * {@code AdminOrderService.createOrder} did.
     */
    public static Order createByAdministrator(UserId userId,
                                              List<OrderLine> lines,
                                              ShippingInformation shippingInformation,
                                              Payment payment,
                                              boolean reviewed,
                                              String note,
                                              Instant doneAt,
                                              PricingPolicy pricingPolicy) {
        Order order = new Order();
        order.userId = userId;
        order.items = lines == null ? null : new ArrayList<>(lines);
        order.shippingInformation = shippingInformation;
        order.payment = payment;
        order.isReviewed = reviewed;
        order.note = note;
        order.doneAt = doneAt;
        order.totalAmount = pricingPolicy.totalFor(order.items);
        order.orderStatus = OrderStatus.PENDING;
        order.addedAt = Instant.now();
        order.modifiedAt = order.addedAt;
        order.recordPayment(TransactionStatus.PENDING, null);
        return order;
    }

    // ------------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------------

    /** The typed identity. Null until Mongo has assigned one on first save. */
    public OrderId orderId() {
        return id == null ? null : OrderId.of(id);
    }

    /** A read-only view — the lines change through {@link #replaceLines}, never in place. */
    public List<OrderLine> getItems() {
        return items == null ? null : Collections.unmodifiableList(items);
    }

    public boolean isOwnedBy(UserId candidate) {
        return userId != null && userId.equals(candidate);
    }

    /**
     * Whether the customer's money has arrived — i.e. whether a cancellation owes a refund.
     *
     * <p>{@code @JsonIgnore} because Jackson auto-detects {@code isX()} as a property, and this
     * aggregate is serialized straight onto the wire. Without it the response grows a
     * {@code "paid"} field that was never there — which is what
     * {@code ResponseDtoWireCompatTest} caught the moment this method was added.
     */
    @JsonIgnore
    public boolean isPaid() {
        return payment != null && payment.isSettled();
    }

    // ------------------------------------------------------------------------
    // Behavior
    // ------------------------------------------------------------------------

    /**
     * Move to {@code target}, recording a payment transaction if the move is one that affects
     * money. This is the only way an order's status changes.
     *
     * @throws IllegalOrderTransitionException if the order cannot reach {@code target} from
     *         where it is — mapped to 409 everywhere except the admin {@code set-*} endpoints,
     *         which have always answered {@code 200 false} and still do
     */
    public void transitionTo(OrderStatus target) {
        applyTransition(target);
        TxEffect effect = TX_EFFECTS.get(target);
        if (effect != null) {
            recordPayment(effect.status(), effect.rawResponse());
        }
        touch();
    }

    /**
     * The customer cancels. A paid order cannot simply vanish — it goes to
     * {@code PENDING_REFUND} with the refund obligation recorded against its payment; an
     * unpaid one is cancelled outright.
     *
     * @throws OrderCannotBeCancelledException once the order is past {@code PROCESSING}
     */
    public void cancel(String reason) {
        if (orderStatus != OrderStatus.PENDING && orderStatus != OrderStatus.PROCESSING) {
            throw new OrderCannotBeCancelledException(orderStatus);
        }

        if (isPaid()) {
            payment.initiateRefund(totalAmount, "Refund initiated for order: " + id);
            applyTransition(OrderStatus.PENDING_REFUND);
        } else {
            applyTransition(OrderStatus.CANCELLED);
        }

        this.note = reason;
        touch();
    }

    /**
     * The payment gateway reported on a checkout. A success moves the order into fulfilment;
     * a failure returns it to awaiting payment, which is why {@code PROCESSING → PENDING} is
     * a legal edge rather than a special case hidden in here.
     */
    public void recordGatewayResult(String gatewayTransactionId, boolean succeeded, String rawResponse) {
        recordPayment(new PaymentTransaction(
                gatewayTransactionId,
                succeeded ? TransactionStatus.SUCCESSFUL : TransactionStatus.FAILED,
                totalAmount,
                rawResponse,
                Instant.now()));

        OrderStatus target = succeeded ? OrderStatus.PROCESSING : OrderStatus.PENDING;
        if (orderStatus != target) {
            applyTransition(target);
        }
        touch();
    }

    /** Append a transaction for the order's total. Opens a COD payment if there is none. */
    public void recordPayment(TransactionStatus status, String rawResponse) {
        recordPayment(PaymentFactory.newTransaction(status, totalAmount, rawResponse));
    }

    /** Append a transaction the gateway supplied an id for. Opens a COD payment if there is none. */
    public void recordPayment(PaymentTransaction transaction) {
        if (payment == null) {
            payment = PaymentFactory.newCodPayment();
        }
        payment.record(transaction);
    }

    /** The customer has reviewed what they bought; a second review is not another rollup. */
    public void markReviewed() {
        this.isReviewed = true;
        touch();
    }

    // ------------------------------------------------------------------------
    // Administrative amendment
    // ------------------------------------------------------------------------

    /**
     * {@code PUT} — an administrator replaces the whole order. Every field is assigned,
     * including from a null, which is the semantics the endpoint has always had.
     *
     * <p>{@code totalAmount} is the exception: it is <em>derived</em> from the submitted
     * lines rather than taken from the request. That is a deliberate change. The old path
     * assigned the client's {@code totalAmount} and the client's {@code items} independently,
     * so a payload could store lines worth $10 against a total of $10,000 and nothing in the
     * system would notice.
     */
    public void replaceContent(UserId userId,
                               List<OrderLine> lines,
                               ShippingInformation shippingInformation,
                               Payment payment,
                               OrderStatus orderStatus,
                               boolean reviewed,
                               String note,
                               Instant doneAt,
                               PricingPolicy pricingPolicy) {
        this.userId = userId;
        replaceLines(lines, pricingPolicy);
        this.shippingInformation = shippingInformation;
        this.payment = payment;
        this.isReviewed = reviewed;
        this.note = note;
        this.doneAt = doneAt;
        moveTowards(orderStatus);
        touch();
    }

    /**
     * {@code PATCH} — an administrator corrects individual fields. A null argument leaves that
     * field alone, which is the semantics that endpoint has always had.
     */
    public void amend(List<OrderLine> lines,
                      ShippingInformation shippingInformation,
                      Payment payment,
                      OrderStatus orderStatus,
                      Instant doneAt,
                      PricingPolicy pricingPolicy) {
        if (lines != null) {
            replaceLines(lines, pricingPolicy);
        }
        if (shippingInformation != null) {
            this.shippingInformation = shippingInformation;
        }
        if (payment != null) {
            this.payment = payment;
        }
        if (doneAt != null) {
            this.doneAt = doneAt;
        }
        moveTowards(orderStatus);
        touch();
    }

    // ------------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------------

    /**
     * Replaces the lines and re-derives the total from them through the one pricing rule, so
     * an amended order is priced the same way a placed one is.
     */
    private void replaceLines(List<OrderLine> lines, PricingPolicy pricingPolicy) {
        Objects.requireNonNull(pricingPolicy, "amending an order's lines requires a pricing policy");
        this.items = lines == null ? null : new ArrayList<>(lines);
        this.totalAmount = pricingPolicy.totalFor(this.items);
    }

    /**
     * The amendment paths' view of a status change: nothing to do when the payload merely
     * repeats the status the order already has, and otherwise the same guarded transition
     * every other path takes.
     *
     * <p>A null current status is assigned straight through. That is not a hole — it means
     * the document was written before the status field existed, and there is no edge in the
     * table that could describe leaving a state the order was never in.
     */
    private void moveTowards(OrderStatus target) {
        if (target == null || target == orderStatus) {
            return;
        }
        if (orderStatus == null) {
            orderStatus = target;
            return;
        }
        transitionTo(target);
    }

    private void applyTransition(OrderStatus target) {
        orderStatus.assertCanTransitionTo(target);
        orderStatus = target;
    }

    /**
     * Stamps {@code modifiedAt}. {@code @LastModifiedDate} would do this on save anyway, but
     * the services did it by hand and the characterization tests observe it before the save,
     * so the aggregate keeps doing it at the moment the change happens.
     */
    private void touch() {
        this.modifiedAt = Instant.now();
    }
}
