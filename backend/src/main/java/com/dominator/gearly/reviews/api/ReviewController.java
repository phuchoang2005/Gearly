package com.dominator.gearly.reviews.api;

import com.dominator.gearly.dto.MessageResponse;
import com.dominator.gearly.platform.security.AuthenticatedUser;
import com.dominator.gearly.reviews.application.ReviewQueryService;
import com.dominator.gearly.reviews.application.SubmitReviewService;
import com.dominator.gearly.reviews.application.SubmitReviewsCommand;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The customer-facing review endpoints. Same URLs, same query parameters, same responses.
 *
 * <p>Three of the four are public reads, and {@code SecurityConfig} now pins them to
 * {@code GET} by name. That is the fix for the second of the sprint's four security items:
 * {@code /api/reviews/**} was {@code permitAll} as one pattern, which also matched
 * {@code POST /api/reviews/submit-review} — so an anonymous submission reached this class with
 * a null principal and became a 500 instead of a 401. The write is authenticated like every
 * other write now, and the principal stops here: it is unwrapped into a {@link UserId} before
 * the use case is called.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewQueryService reviewQueryService;
    private final SubmitReviewService submitReviewService;
    private final ReviewResponseMapper reviewResponseMapper;

    /**
     * One page of a product's published reviews.
     *
     * <p>The {@code Page} is rebuilt here, at the boundary, from the context's own
     * {@code ReviewPage}: the domain may not name a Spring Data paging type, and the JSON the
     * storefront receives has to stay exactly as it was.
     */
    @GetMapping
    public ResponseEntity<Page<ReviewResponseDTO>> getReviewsByProductId(ProductReviewsDTO dto) {
        ReviewQueryService.PagedReviews found = reviewQueryService.approvedFor(
                ProductId.of(dto.getProductId()), dto.getRating(),
                dto.getPageIndex(), dto.getPageSize(), dto.getSortBy());

        return ResponseEntity.ok(new PageImpl<>(
                reviewResponseMapper.toResponseDtos(found.content()),
                PageRequest.of(found.page(), found.size()),
                found.totalElements()));
    }

    @GetMapping("/best-six")
    public ResponseEntity<List<ReviewResponseDTO>> getBestSixReviews() {
        return ResponseEntity.ok(reviewResponseMapper.toResponseDtos(reviewQueryService.testimonials()));
    }

    @GetMapping("/distribution")
    public ResponseEntity<List<ReviewRatingDTO>> getRatingDistribution(@RequestParam String productId) {
        return ResponseEntity.ok(reviewQueryService.distributionFor(ProductId.of(productId)).stream()
                .map(reviewResponseMapper::toRatingDto)
                .toList());
    }

    @PostMapping("/submit-review")
    public ResponseEntity<MessageResponse> submitReview(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestBody @Valid CreateReviewsRequestDTO dto
    ) {
        submitReviewService.submit(authUser.id(), toCommand(dto));
        return ResponseEntity.ok(new MessageResponse("Reviews submitted successfully"));
    }

    private static SubmitReviewsCommand toCommand(CreateReviewsRequestDTO dto) {
        return new SubmitReviewsCommand(dto.getOrderId(), dto.getReviews().stream()
                .map(line -> new SubmitReviewsCommand.Line(
                        line.getProductId(), line.getSubject(), line.getComment(), line.getRating()))
                .toList());
    }
}
