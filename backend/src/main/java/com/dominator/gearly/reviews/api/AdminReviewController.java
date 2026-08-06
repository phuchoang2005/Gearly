package com.dominator.gearly.reviews.api;

import com.dominator.gearly.reviews.application.AdminReviewService;
import com.dominator.gearly.reviews.domain.ReviewStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The moderation console. Same four URLs and the same response bodies.
 *
 * <p>What changed behind them: approving an already-approved review, or rejecting an
 * already-rejected one, is a <b>409</b> rather than a silent no-op that used to re-save the
 * same status. That matters now — {@code APPROVED} is what makes a product's rating move, so a
 * moderator clicking twice would otherwise count the same stars twice.
 *
 * <p>{@code @PreAuthorize} repeats the {@code /api/admin/**} URL rule at the method level; see
 * {@code AdminUserController} for why both.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    private final AdminReviewService reviewService;
    private final ReviewResponseMapper reviewResponseMapper;

    @GetMapping
    public ResponseEntity<List<AdminReviewResponseDTO>> getAll() {
        return ResponseEntity.ok(reviewResponseMapper.toAdminDtos(reviewService.getAllReviews()));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<AdminReviewResponseDTO>> getReviewsByStatus(@PathVariable ReviewStatus status) {
        return ResponseEntity.ok(reviewResponseMapper.toAdminDtos(reviewService.getReviewsByStatus(status)));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AdminReviewResponseDTO> approve(@PathVariable String id) {
        return ResponseEntity.ok(reviewResponseMapper.toAdminDto(reviewService.approveReview(id)));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<AdminReviewResponseDTO> reject(@PathVariable String id) {
        return ResponseEntity.ok(reviewResponseMapper.toAdminDto(reviewService.rejectReview(id)));
    }
}
