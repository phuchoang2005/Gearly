package com.dominator.bookify.controller.user;

import com.dominator.bookify.dto.BookReviewsDTO;
import com.dominator.bookify.dto.CreateReviewsRequestDTO;
import com.dominator.bookify.dto.ReviewRatingDTO;
import com.dominator.bookify.dto.ReviewResponseDTO;
import com.dominator.bookify.model.Review;
import com.dominator.bookify.security.AuthenticatedUser;
import com.dominator.bookify.service.user.ReviewService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public ResponseEntity<?> getReviewsByBookId(BookReviewsDTO dto) {
        try {
            Page<ReviewResponseDTO> reviews = reviewService.getApprovedReviews(dto);

            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/best-six")
    public ResponseEntity<?> getBestSixReviews() {
        try {
            List<ReviewResponseDTO> reviews = reviewService.getSixBestReviews();
            return ResponseEntity.ok(reviews);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/distribution")
    public ResponseEntity<?> getRatingDistribution(@RequestParam String bookId) {
        try {
            List<ReviewRatingDTO> dist = reviewService.getRatingDistribution(bookId);
            return ResponseEntity.ok(dist);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/submit-review")
    public ResponseEntity<?> submitReview(@AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody CreateReviewsRequestDTO dto) {
        try {
            reviewService.createReview(authUser, dto);
            return ResponseEntity.ok(Map.of("message", "Reviews submitted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}
