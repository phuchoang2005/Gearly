package com.dominator.bookify.controller.admin;

import java.util.List;

import com.dominator.bookify.dto.AdminReviewResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.dominator.bookify.model.Review;
import com.dominator.bookify.model.ReviewStatus;
import com.dominator.bookify.service.admin.AdminReviewService;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    @Autowired
    private AdminReviewService reviewService;

    @GetMapping
    public ResponseEntity<List<AdminReviewResponseDTO>> getAll() {
        return ResponseEntity.ok(reviewService.getAllReviews());
    }

    @GetMapping("/status/{status}")
    public List<Review> getReviewsByStatus(@PathVariable ReviewStatus status) {
        return reviewService.getReviewsByStatus(status);
    }

    @PostMapping("/{id}/approve")
    public Review approve(@PathVariable String id) {
        return reviewService.approveReview(id);
    }

    @PostMapping("/{id}/reject")
    public Review reject(@PathVariable String id) {
        return reviewService.rejectReview(id);
    }
}
