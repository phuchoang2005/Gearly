package com.dominator.gearly.model;

import com.dominator.gearly.ordering.domain.TransactionStatus;
import com.dominator.gearly.shared.domain.Money;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * One movement of money against an order — the initial pending charge, the gateway's
 * settlement callback, a refund.
 *
 * <p>The {@code @Document} annotation is a copy-paste artifact: transactions are only ever
 * embedded in a {@link Payment}. S10 drops it when this becomes {@code PaymentTransaction}
 * in {@code ordering.domain}.
 */
@Document(collection = "transaction")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Transaction {
    private String transactionId;
    private TransactionStatus status;
    private Money amount = Money.ZERO;
    private String rawResponse;
    @CreatedDate
    private Instant createdAt;
}
