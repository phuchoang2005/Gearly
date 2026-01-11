package com.dominator.bookify.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;


import java.time.Instant;

@Document(collection = "transaction")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Transaction {
    private String transactionId;
    private TransactionStatus status;
    private double amount;
    private String rawResponse;
    @CreatedDate
    private Instant createdAt;
}
