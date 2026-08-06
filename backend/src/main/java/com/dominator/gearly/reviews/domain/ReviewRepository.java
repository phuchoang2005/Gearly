package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.ReviewId;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The reviews context's repository port, stated in its own vocabulary: typed ids and value
 * objects in, aggregates out. The MongoDB adapter behind it is {@code MongoReviewRepository}.
 *
 * <p>Replaces a Spring Data interface whose method names encoded the storage —
 * {@code findByProductIdAndStatusAndRating(ObjectId, …)} took the BSON type, so every caller
 * had to know that a review's {@code productId} is an {@code ObjectId} while the same id is a
 * string on an order line. That knowledge lives in the adapter now, and nowhere else.
 */
public interface ReviewRepository {

    Optional<Review> findById(ReviewId id);

    /** Saves, then publishes whatever the aggregate recorded — see the adapter. */
    Review save(Review review);

    List<Review> saveAll(List<Review> reviews);

    List<Review> findAll();

    List<Review> findByStatus(ReviewStatus status);

    /**
     * The public review list for a product: approved only, newest-ish first.
     *
     * @param rating an optional star filter; {@code null} means every rating
     * @param sortBy the field to sort on, descending — as the storefront has always supplied it
     */
    ReviewPage findApproved(ProductId productId, Rating rating, int page, int size, String sortBy);

    /** Candidates for the storefront's testimonial strip: approved reviews at {@code rating}. */
    List<Review> findTopRated(Rating rating, int limit);

    /**
     * How many approved reviews a product has at each star count.
     *
     * <p>Returns a {@link Rating}-keyed map, so a bucket outside 1–5 — which a legacy document
     * could produce — is dropped by the adapter rather than by every caller. That is the
     * behaviour the S8 suite pins: an out-of-range bucket is ignored <em>before</em> the total
     * is summed, so it cannot skew the percentages.
     */
    Map<Rating, Long> ratingTally(ProductId productId);
}
