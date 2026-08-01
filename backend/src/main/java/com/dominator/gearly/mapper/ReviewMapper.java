package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.AdminReviewResponseDTO;
import com.dominator.gearly.dto.CreateReviewRequestDTO;
import com.dominator.gearly.dto.ReviewResponseDTO;
import com.dominator.gearly.model.Review;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

/**
 * Maps {@link Review} entities to public and admin DTOs and builds review
 * entities from create requests. The book title and user name are resolved by
 * the caller (batched lookups) and passed in.
 */
@Component
public class ReviewMapper {

    /** Customer-facing review with the author's display name. */
    public ReviewResponseDTO toResponseDto(Review review, String userName) {
        return new ReviewResponseDTO(
                review.getId(),
                review.getRating(),
                review.getSubject(),
                review.getComment(),
                review.getAddedAt(),
                userName);
    }

    /** Admin moderation view with resolved book title and user name. */
    public AdminReviewResponseDTO toAdminDto(Review review, String bookTitle, String userName) {
        return new AdminReviewResponseDTO(
                review.getId(),
                bookTitle,
                userName,
                review.getRating(),
                review.getSubject(),
                review.getComment(),
                review.getStatus(),
                review.getAddedAt(),
                review.getModifiedAt());
    }

    public Review toEntity(CreateReviewRequestDTO dto, String orderId, String userId) {
        Review review = new Review();
        review.setBookId(new ObjectId(dto.getBookId()));
        review.setOrderId(new ObjectId(orderId));
        review.setUserId(new ObjectId(userId));
        review.setRating(dto.getRating());
        review.setSubject(dto.getSubject());
        review.setComment(dto.getComment());
        return review;
    }
}
