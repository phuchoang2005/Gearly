package com.dominator.gearly.reviews.application;

import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.catalog.domain.ProductSnapshotPort;
import com.dominator.gearly.identity.domain.UserDirectory;
import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewNotFoundException;
import com.dominator.gearly.reviews.domain.ReviewRepository;
import com.dominator.gearly.reviews.domain.ReviewStatus;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.ReviewId;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Moderation: the queue, and the two decisions a moderator can make about a review.
 *
 * <p>Both decisions are the aggregate's now. {@code setStatus(id, status)} — one private method
 * that assigned whatever constant it was handed — is gone, and with it the ability to approve an
 * approved review, reject a rejected one, or put a moderated review back to {@code PENDING}.
 * That mattered little while nothing depended on the status; it matters a great deal now that
 * {@code APPROVED} is what moves a product's rating.
 *
 * <p>Both neighbouring contexts are reached through their published ports —
 * {@link ProductSnapshotPort} for a title, {@link UserDirectory} for a name — so this screen
 * holds neither a {@code Product} nor a {@code User}.
 */
@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ReviewRepository reviews;
    private final ProductSnapshotPort catalog;
    private final UserDirectory users;

    public List<ModeratedReview> getAllReviews() {
        return decorate(reviews.findAll());
    }

    public List<ModeratedReview> getReviewsByStatus(ReviewStatus status) {
        return decorate(reviews.findByStatus(status));
    }

    /** @throws com.dominator.gearly.reviews.domain.IllegalReviewTransitionException with a 409 if it is already approved */
    public ModeratedReview approveReview(String id) {
        return moderate(id, Review::approve);
    }

    /** @throws com.dominator.gearly.reviews.domain.IllegalReviewTransitionException with a 409 if it is already rejected */
    public ModeratedReview rejectReview(String id) {
        return moderate(id, Review::reject);
    }

    private ModeratedReview moderate(String id, Consumer<Review> decision) {
        Review review = reviews.findById(ReviewId.of(id))
                .orElseThrow(ReviewNotFoundException::new);
        decision.accept(review);
        return decorateOne(reviews.save(review));
    }

    /**
     * Resolves the product title and author name for a batch — two queries for the whole table,
     * where the old code ran one user lookup per row.
     */
    private List<ModeratedReview> decorate(List<Review> found) {
        List<ProductId> productIds = found.stream().map(Review::getProductId).distinct().toList();
        List<UserId> userIds = found.stream().map(Review::getUserId).distinct().toList();

        Map<ProductId, String> titles = catalog.snapshotsOf(productIds).stream()
                .collect(Collectors.toMap(CatalogSnapshot::productId, CatalogSnapshot::title));
        Map<UserId, String> names = users.displayNamesOf(userIds);

        return found.stream()
                .map(review -> new ModeratedReview(review,
                        titles.get(review.getProductId()),
                        names.get(review.getUserId())))
                .toList();
    }

    /** Single-review variant, for the two moderation responses. */
    private ModeratedReview decorateOne(Review review) {
        return new ModeratedReview(review,
                catalog.findSnapshot(review.getProductId()).map(CatalogSnapshot::title).orElse(null),
                users.displayNameOf(review.getUserId()).orElse(null));
    }

    /**
     * A review as the moderation console sees it: with the product it is about and the person
     * who wrote it resolved to names. Either may be {@code null} — a product can be delisted and
     * an account deleted without the review going anywhere — and the api layer supplies the
     * dash the console has always shown in their place.
     */
    public record ModeratedReview(Review review, String productTitle, String authorName) {
    }
}
