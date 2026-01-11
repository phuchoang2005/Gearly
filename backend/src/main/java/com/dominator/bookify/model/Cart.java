package com.dominator.bookify.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Document(collection = "carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    @Id
    private String id;

    private String userId;
    private String guestId;

    private List<CartItem> items = new ArrayList<>();

    @CreatedDate
    private Date createdAt;
    @LastModifiedDate
    private Date updatedAt;
}