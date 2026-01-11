package com.dominator.bookify.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "reviews")
public class Review {
    @Id
    private String id;
    private ObjectId bookId;
    private ObjectId orderId;
    private ObjectId userId;
    private int rating;
    private String subject;
    private String comment;
    private ReviewStatus status = ReviewStatus.PENDING;
    @CreatedDate
    private String addedAt;
    @LastModifiedDate
    private String modifiedAt;
}
