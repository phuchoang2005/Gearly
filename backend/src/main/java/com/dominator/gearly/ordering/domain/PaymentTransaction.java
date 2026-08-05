package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.Money;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.springframework.data.annotation.PersistenceCreator;

import java.time.Instant;

/**
 * One movement of money against an order — the opening pending charge, the gateway's
 * settlement callback, a refund. Immutable: a transaction is a record of something that
 * already happened, so there is nothing about it to change afterwards.
 *
 * <p>Was {@code model.Transaction}. Two things came off in the move:
 *
 * <ul>
 *   <li>{@code @Document(collection = "transaction")} — transactions are only ever embedded
 *       in a {@link Payment}, and that collection does not exist.</li>
 *   <li>{@code @CreatedDate} on {@code createdAt} — Spring Data auditing populates the
 *       <em>root</em> entity only, so on an embedded type the annotation never fired. The
 *       field was, and still is, set explicitly by whoever creates the transaction; the
 *       annotation just made it look automatic.</li>
 * </ul>
 */
@Getter
public class PaymentTransaction {

    private final String transactionId;
    private final TransactionStatus status;
    private final Money amount;
    private final String rawResponse;
    private final Instant createdAt;

    @PersistenceCreator
    @JsonCreator
    public PaymentTransaction(
            @JsonProperty("transactionId") String transactionId,
            @JsonProperty("status") TransactionStatus status,
            @JsonProperty("amount") Money amount,
            @JsonProperty("rawResponse") String rawResponse,
            @JsonProperty("createdAt") Instant createdAt) {
        this.transactionId = transactionId;
        this.status = status;
        // a legacy document may have no amount at all; it read as zero before, and does now
        this.amount = amount == null ? Money.ZERO : amount;
        this.rawResponse = rawResponse;
        this.createdAt = createdAt;
    }

    /** {@code @JsonIgnore} — a derived predicate, not a field the wire has ever carried. */
    @JsonIgnore
    public boolean isSuccessful() {
        return status == TransactionStatus.SUCCESSFUL;
    }
}
