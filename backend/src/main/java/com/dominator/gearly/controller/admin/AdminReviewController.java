package com.dominator.gearly.controller.admin;

import java.util.List;

import com.dominator.gearly.dto.AdminReviewResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dominator.gearly.model.Review;
import com.dominator.gearly.model.ReviewStatus;
import com.dominator.gearly.service.admin.AdminReviewService;

// TODO(S4): Review entity responses should become response DTOs alongside the mapper layer.
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final AdminReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<AdminReviewResponseDTO>> getAll() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Review>> getReviewsByStatus(@PathVariable ReviewStatus status) {
        return ResponseEntity.ok(reviewService.getReviewsByStatus(status));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<Review> approve(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.approveReview(id));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Review> reject(@PathVariable String id) {
        return ResponseEntity.ok(reviewService.rejectReview(id));
    }
}
