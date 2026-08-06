package com.dominator.gearly.reviews.infrastructure;

import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Map;

/**
 * Spring Data's view of the reviews collection. The raw {@code String} parameters are the point:
 * this is the layer that knows what BSON type a review's product, order and author ids are
 * stored as — since S12, the same plain string an order or a cart holds.
 *
 * <p>They were {@code ObjectId} until S12; see {@code Review}'s persistence note and
 * {@code data/seed/migrate.js} step 11.
 */
interface SpringDataReviewRepository extends MongoRepository<Review, String> {

    Page<Review> findByProductIdAndStatus(String productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByProductIdAndStatusAndRating(String productId, ReviewStatus status,
                                                   int rating, Pageable pageable);

    /**
     * Counts per star, approved only — the same pipeline the storefront's histogram has always
     * been drawn from. It matches on the raw stored value, which is why normalizing the id had
     * to change this signature in the same commit.
     */
    @Aggregation(pipeline = {
            "{ $match: { productId: ?0, status: 'APPROVED' } }",
            "{ $group: { _id: '$rating' , count: { $sum: 1 } } }"
    })
    List<Map<String, Object>> getRatingDistribution(String productId);

    List<Review> findReviewByStatus(ReviewStatus status);

    @Query("{ 'rating': ?0, 'status': ?1 }")
    List<Review> findTopByRatingAndStatusGroupedByUser(int rating, ReviewStatus status, Pageable pageable);
}
