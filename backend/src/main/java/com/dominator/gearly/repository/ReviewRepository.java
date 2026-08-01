package com.dominator.gearly.repository;

import com.dominator.gearly.model.Review;
import com.dominator.gearly.model.ReviewStatus;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {
    Page<Review> findByProductIdAndStatus(ObjectId productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByProductIdAndStatusAndRating(ObjectId productId, ReviewStatus status, int rating, Pageable pageable);

    @Aggregation(pipeline = {
            "{ $match: { productId: ?0, status: 'APPROVED' } }",
            "{ $group: { _id: '$rating' , count: { $sum: 1 } } }"
    })
    List<Map<String, Object>> getRatingDistribution(ObjectId productId);

    List<Review> findReviewByStatus(ReviewStatus status);

    @Query("{ 'rating': ?0, 'status': ?1 }")
    List<Review> findTopByRatingAndStatusGroupedByUser(int rating, ReviewStatus status, Pageable pageable);
}
