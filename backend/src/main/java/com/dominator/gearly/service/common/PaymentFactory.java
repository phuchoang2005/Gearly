package com.dominator.gearly.service.common;

import com.dominator.gearly.model.Order;
import com.dominator.gearly.model.Payment;
import com.dominator.gearly.model.Transaction;
import com.dominator.gearly.ordering.domain.TransactionStatus;
import com.dominator.gearly.shared.domain.Money;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Builds Payment / Transaction records for orders, centralizing the COD-payment
 * and transaction-append logic that was previously duplicated in the admin order
 * service.
 */
@Service
public class PaymentFactory {

    public Payment newCodPayment() {
        Payment payment = new Payment();
        payment.setMethod("cod");
        payment.setTransactions(new ArrayList<>());
        return payment;
    }

    public Transaction newTransaction(TransactionStatus status, Money amount, String rawResponse) {
        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID().toString());
        tx.setStatus(status);
        tx.setAmount(amount);
        tx.setRawResponse(rawResponse);
        tx.setCreatedAt(Instant.now());
        return tx;
    }

    /** Ensure the order has a (COD) payment and append a transaction for its total amount. */
    public void appendTransaction(Order order, TransactionStatus status, String rawResponse) {
        Payment payment = order.getPayment();
        if (payment == null) {
            payment = newCodPayment();
            order.setPayment(payment);
        }
        if (payment.getTransactions() == null) {
            payment.setTransactions(new ArrayList<>());
        }
        payment.getTransactions().add(newTransaction(status, order.getTotalAmount(), rawResponse));
    }
}
