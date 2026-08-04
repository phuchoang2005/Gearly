package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.Money;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.data.annotation.PersistenceCreator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The payment record of an order: how it is being paid for, and every
 * {@link PaymentTransaction} that has accumulated against it.
 *
 * <p>Inside the {@link Order} boundary — it is never loaded or saved on its own, and its
 * {@code @Document(collection = "payment")} came off in the move for the same reason it came
 * off {@link PaymentTransaction}.
 *
 * <h2>The immutable-list bug this fixes</h2>
 * {@code CustomerOrderService.buildInitialPayment} built the opening transaction list with
 * {@code List.of(...)}, so appending to a freshly placed order threw
 * {@code UnsupportedOperationException} — only a round trip through Mongo turned it into an
 * {@code ArrayList} and made the cancel and gateway-callback paths work. The S8
 * characterization suite pinned that as a {@code KNOWN BUG}. The list is owned here now, is
 * always mutable, and is only ever handed out {@linkplain Collections#unmodifiableList
 * unmodifiable}, so no caller can append behind the aggregate's back either.
 *
 * <h2>Why {@code method} is still a String</h2>
 * The stored values are inconsistent — {@code "cod"} from the customer path, {@code "MOMO"}
 * in fixtures — so an enum would need a case-folding converter and would turn any
 * unrecognized legacy value into an unreadable document. S13 introduces it along with the
 * {@code PaymentGateway} port, where the set of supported methods is actually decided.
 */
public class Payment {

    @Getter
    private final String method;

    private final List<PaymentTransaction> transactions;

    @PersistenceCreator
    @JsonCreator
    public Payment(@JsonProperty("method") String method,
                   @JsonProperty("transactions") List<PaymentTransaction> transactions) {
        this.method = method;
        this.transactions = transactions == null ? new ArrayList<>() : new ArrayList<>(transactions);
    }

    /** A read-only view. Append through {@link #record} so the aggregate stays in control. */
    public List<PaymentTransaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    /**
     * Whether any transaction has settled — i.e. whether the customer's money has arrived.
     *
     * <p>{@code @JsonIgnore}: Jackson reads {@code isX()} as a property, and a payment is
     * serialized as part of the order response. A derived predicate is not part of that
     * contract.
     */
    @JsonIgnore
    public boolean isSettled() {
        return transactions.stream().anyMatch(PaymentTransaction::isSuccessful);
    }

    void record(PaymentTransaction transaction) {
        transactions.add(transaction);
    }

    /**
     * Opens a refund for {@code amount}. The money does not move here — this records the
     * obligation, which an admin later settles by moving the order to {@code REFUNDED}.
     *
     * @param rawResponse the human-readable note the existing data carries on this row; the
     *                    plan's signature omits it, but every refund transaction in the
     *                    collection has one and dropping it would lose the order reference
     */
    void initiateRefund(Money amount, String rawResponse) {
        record(PaymentFactory.newTransaction(TransactionStatus.PENDING_REFUND, amount, rawResponse));
    }
}
