package com.dominator.bookify.dto;

import org.bson.types.ObjectId;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class HandleReviewStatusDTO {
    private String id;
    private ObjectId bookId;
    private ObjectId userId;
    private int rating;
    private String subject;
    private String comment;
    private String status;
    private String addedAt;
    private String modifiedAt;

}
