package com.dominator.gearly.model;

import lombok.Getter;
import lombok.Setter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "products")
public class Product {
    @Id
    private String id;
    private String title;
    private List<String> authors;
    private String description;
    private double price;
    private double originalPrice;
    private String condition;
    private int stock;
    private List<ObjectId> categoryIds;
    private List<Image> images;

    @Transient
    private List<String> categoryNames;

    // Rating
    private double averageRating;
    private int ratingCount;
    private int totalRating;

    @CreatedDate
    private Instant addedAt;

    @LastModifiedDate
    private Instant modifiedAt;
}