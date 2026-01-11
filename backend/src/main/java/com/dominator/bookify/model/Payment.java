package com.dominator.bookify.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

import java.util.List;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "payment")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Payment {
    private String method;
    private List<Transaction> transactions;
}
