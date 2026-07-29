package com.dominator.bookify.controller.user;

import com.dominator.bookify.dto.BookReviewsDTO;
import com.dominator.bookify.dto.CreateReviewsRequestDTO;
import com.dominator.bookify.dto.MessageResponse;
import com.dominator.bookify.dto.ReviewRatingDTO;
import com.dominator.bookify.dto.ReviewResponseDTO;
import com.dominator.bookify.security.AuthenticatedUser;
import com.dominator.bookify.service.user.ReviewService;
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
    public ResponseEntity<Page<ReviewResponseDTO>> getReviewsByBookId(BookReviewsDTO dto) {
        return ResponseEntity.ok(reviewService.getApprovedReviews(dto));
    }

    @GetMapping("/best-six")
    public ResponseEntity<List<ReviewResponseDTO>> getBestSixReviews() {
        return ResponseEntity.ok(reviewService.getSixBestReviews());
    }

    @GetMapping("/distribution")
    public ResponseEntity<List<ReviewRatingDTO>> getRatingDistribution(@RequestParam String bookId) {
        return ResponseEntity.ok(reviewService.getRatingDistribution(bookId));
    }

    @PostMapping("/submit-review")
    public ResponseEntity<MessageResponse> submitReview(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody @Valid CreateReviewsRequestDTO dto
    ) {
        reviewService.createReview(authUser, dto);
        return ResponseEntity.ok(new MessageResponse("Reviews submitted successfully"));
    }
}
