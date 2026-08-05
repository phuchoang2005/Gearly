
package com.dominator.gearly.reviews.api;

import com.dominator.gearly.reviews.domain.ReviewStatus;
import lombok.AllArgsConstructor;

import java.time.Instant;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminReviewResponseDTO {
    private String id;

    // instead of raw ObjectId, expose productTitle
    private String productTitle;

    // instead of raw ObjectId, expose userName
    private String userName;

    private int rating;
    private String subject;
    private String comment;
    private ReviewStatus status;
    private Instant addedAt;
    private Instant modifiedAt;
}
