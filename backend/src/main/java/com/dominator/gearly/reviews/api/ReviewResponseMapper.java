package com.dominator.gearly.reviews.api;

import com.dominator.gearly.reviews.application.AdminReviewService;
import com.dominator.gearly.reviews.application.AuthoredReview;
import com.dominator.gearly.reviews.application.ReviewQueryService;
import com.dominator.gearly.reviews.domain.Review;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Turns reviews into the two JSON shapes the frontends read. Was {@code mapper.ReviewMapper}.
 *
 * <p><b>The wire format is unchanged.</b> {@code rating} is a bare number as it always was —
 * {@code Rating} carries {@code @JsonValue}, so adopting the value object on the aggregate did
 * not change a byte of the response.
 *
 * <p>The two "we do not know" placeholders live here rather than in the services that resolve
 * the names, because they are display decisions and they differ: the storefront has always
 * shown {@code "Unknown User"} beside a review whose author is gone, and the moderation console
 * an em dash. Both application services hand out {@code null} and let this class phrase it.
 */
@Component
public class ReviewResponseMapper {

    /** What the storefront shows when a review's author no longer has an account. */
    private static final String UNKNOWN_AUTHOR = "Unknown User";

    /** What the moderation console shows in place of a missing product or author. */
    private static final String MISSING = "—";

    /** Customer-facing review with the author's display name. */
    public ReviewResponseDTO toResponseDto(AuthoredReview authored) {
        Review review = authored.review();
        return new ReviewResponseDTO(
                review.getId(),
                review.getRating().toInt(),
                review.getSubject(),
                review.getComment(),
                review.getAddedAt(),
                authored.authorName() == null ? UNKNOWN_AUTHOR : authored.authorName());
    }

    public List<ReviewResponseDTO> toResponseDtos(List<AuthoredReview> authored) {
        return authored.stream().map(this::toResponseDto).toList();
    }

    /** Admin moderation view with resolved product title and author name. */
    public AdminReviewResponseDTO toAdminDto(AdminReviewService.ModeratedReview moderated) {
        Review review = moderated.review();
        return new AdminReviewResponseDTO(
                review.getId(),
                moderated.productTitle() == null ? MISSING : moderated.productTitle(),
                moderated.authorName() == null ? MISSING : moderated.authorName(),
                review.getRating().toInt(),
                review.getSubject(),
                review.getComment(),
                review.getStatus(),
                review.getAddedAt(),
                review.getModifiedAt());
    }

    public List<AdminReviewResponseDTO> toAdminDtos(List<AdminReviewService.ModeratedReview> moderated) {
        return moderated.stream().map(this::toAdminDto).toList();
    }

    /** One bar of the star histogram. */
    public ReviewRatingDTO toRatingDto(ReviewQueryService.RatingShare share) {
        return new ReviewRatingDTO(share.stars(), share.count(), share.percentage());
    }
}
