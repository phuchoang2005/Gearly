package com.dominator.bookify.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "orders")
public class Order {
    @Id
    private String id;
    private String userId;
    @Indexed(name = "idx_items_bookdId")
    private List<OrderItem> items;
    private double totalAmount;
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
}
