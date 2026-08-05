package com.dominator.gearly.reviews.infrastructure;

import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewStatus;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Map;

/**
 * Spring Data's view of the reviews collection. The {@code ObjectId} parameters are the point:
 * this is the layer that knows a review's product, order and author ids are stored as BSON
 * {@code ObjectId}s while the same typed ids are plain strings on an order or a cart.
 */
interface SpringDataReviewRepository extends MongoRepository<Review, String> {

    Page<Review> findByProductIdAndStatus(ObjectId productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByProductIdAndStatusAndRating(ObjectId productId, ReviewStatus status,
                                                   int rating, Pageable pageable);

    /**
     * Counts per star, approved only — the same pipeline the storefront's histogram has always
     * been drawn from, and the reason the id fields must stay {@code ObjectId}s: this matches
     * on the raw stored value.
     */
    @Aggregation(pipeline = {
            "{ $match: { productId: ?0, status: 'APPROVED' } }",
            "{ $group: { _id: '$rating' , count: { $sum: 1 } } }"
    })
    List<Map<String, Object>> getRatingDistribution(ObjectId productId);

    List<Review> findReviewByStatus(ReviewStatus status);

    @Query("{ 'rating': ?0, 'status': ?1 }")
    List<Review> findTopByRatingAndStatusGroupedByUser(int rating, ReviewStatus status, Pageable pageable);
}
