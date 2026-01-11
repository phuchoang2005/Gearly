// src/main/java/com/dominator/bookify/dto/ReviewResponseDTO.java
package com.dominator.bookify.dto;

import com.dominator.bookify.model.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminReviewResponseDTO {
    private String id;

    // instead of raw ObjectId, expose bookTitle
    private String bookTitle;

    // instead of raw ObjectId, expose userName
    private String userName;

    private int rating;
    private String subject;
    private String comment;
    private ReviewStatus status;
    private String addedAt;
    private String modifiedAt;
}
