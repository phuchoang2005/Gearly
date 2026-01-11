package com.dominator.bookify.model;

import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "orderItem")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class OrderItem {
    private String bookId;
    private String title;
    private double price;
    private String imageUrl;
    private int quantity;
}
