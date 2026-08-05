package com.dominator.gearly.controller.user;

import com.dominator.gearly.dto.ProductReviewsDTO;
import com.dominator.gearly.dto.CreateReviewsRequestDTO;
import com.dominator.gearly.dto.MessageResponse;
import com.dominator.gearly.dto.ReviewRatingDTO;
import com.dominator.gearly.dto.ReviewResponseDTO;
import com.dominator.gearly.platform.security.AuthenticatedUser;
import com.dominator.gearly.service.user.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<Page<ReviewResponseDTO>> getReviewsByProductId(ProductReviewsDTO dto) {
        return ResponseEntity.ok(reviewService.getApprovedReviews(dto));
    }

    @GetMapping("/best-six")
    public ResponseEntity<List<ReviewResponseDTO>> getBestSixReviews() {
        return ResponseEntity.ok(reviewService.getSixBestReviews());
    }

    @GetMapping("/distribution")
    public ResponseEntity<List<ReviewRatingDTO>> getRatingDistribution(@RequestParam String productId) {
        return ResponseEntity.ok(reviewService.getRatingDistribution(productId));
    }

    @PostMapping("/submit-review")
    public ResponseEntity<MessageResponse> submitReview(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody @Valid CreateReviewsRequestDTO dto
    ) {
        reviewService.createReview(authUser.id(), dto);
        return ResponseEntity.ok(new MessageResponse("Reviews submitted successfully"));
    }
}
