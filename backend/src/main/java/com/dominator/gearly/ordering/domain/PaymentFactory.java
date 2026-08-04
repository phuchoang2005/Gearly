package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.Money;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Assembles {@link Payment} and {@link PaymentTransaction} records.
 *
 * <p>Relocated from {@code service/common/PaymentFactory}, where it was a {@code @Service}.
 * It never was one: it holds no state, calls no collaborator, and does nothing but build
 * domain objects — the definition of a domain factory that had simply been filed under
 * services. Here it is a static utility, both because it needs no injection and because the
 * domain may not carry {@code @Service} at all ({@code org.springframework.stereotype} is on
 * ArchUnit's banned list for a domain package).
 *
 * <p>Its old {@code appendTransaction(Order, …)} method is gone: appending to an order is
 * {@code Order.recordPayment(…)} now, so the aggregate — not a helper anyone can call — is
 * what decides when a transaction is added and for how much.
 */
public final class PaymentFactory {

    private PaymentFactory() {
    }

    /**
     * The payment an order falls back to when one is needed and none exists — an admin-created
     * order, or a legacy document saved without one. Cash on delivery, as before.
     */
    public static Payment newCodPayment() {
        return new Payment("cod", new ArrayList<>());
    }

    /** A payment opened with a single pending charge for the order's total. */
    public static Payment newPendingPayment(String method, Money amount) {
        Payment payment = new Payment(method, new ArrayList<>());
        // the rawResponse text is what is already in the collection — keep it byte-identical
        payment.record(newTransaction(TransactionStatus.PENDING, amount,
                "Pending payment: " + amount.toDouble()));
        return payment;
    }

    public static PaymentTransaction newTransaction(TransactionStatus status, Money amount, String rawResponse) {
        return new PaymentTransaction(UUID.randomUUID().toString(), status, amount, rawResponse, Instant.now());
    }
}
