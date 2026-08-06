package com.dominator.gearly.reviews.infrastructure;

import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewPage;
import com.dominator.gearly.reviews.domain.ReviewRepository;
import com.dominator.gearly.reviews.domain.ReviewStatus;
import com.dominator.gearly.shared.domain.DomainEvent;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.ReviewId;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The MongoDB adapter behind {@link ReviewRepository}. The only class in the reviews context
 * that knows a review is stored in MongoDB, what BSON type its three ids take, or that the
 * star histogram is an aggregation pipeline.
 */
@Repository
@RequiredArgsConstructor
public class MongoReviewRepository implements ReviewRepository {

    private final SpringDataReviewRepository reviews;
    private final ApplicationEventPublisher events;

    @Override
    public Optional<Review> findById(ReviewId id) {
        return reviews.findById(id.value());
    }

    /**
     * Writes the aggregate, then publishes what it recorded — the contract
     * {@code MongoOrderRepository.save} established and {@code MongoUserRepository} follows.
     *
     * <p>It matters more here than anywhere else: {@code ReviewApproved} is what makes a
     * product's rating move, so publishing before the write would let a failed save leave the
     * catalog counting a review that was never published, and forgetting to publish would leave
     * an approved review that never reaches an average. One place does it, after the write, for
     * every moderation path.
     */
    @Override
    public Review save(Review review) {
        Review saved = reviews.save(review);
        publish(saved);
        return saved;
    }

    @Override
    public List<Review> saveAll(List<Review> toSave) {
        List<Review> saved = reviews.saveAll(toSave);
        saved.forEach(this::publish);
        return saved;
    }

    @Override
    public List<Review> findAll() {
        return reviews.findAll();
    }

    @Override
    public List<Review> findByStatus(ReviewStatus status) {
        return reviews.findReviewByStatus(status);
    }

    @Override
    public ReviewPage findApproved(ProductId productId, Rating rating,
                                   int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, sortBy));

        Page<Review> result = rating == null
                ? reviews.findByProductIdAndStatus(productId.value(), ReviewStatus.APPROVED, pageable)
                : reviews.findByProductIdAndStatusAndRating(
                        productId.value(), ReviewStatus.APPROVED, rating.toInt(), pageable);

        return new ReviewPage(result.getContent(), page, size, result.getTotalElements());
    }

    /**
     * {@inheritDoc}
     *
     * <p>The {@code limit} is a page size rather than a cap on the answer, exactly as before:
     * the caller asks for more than it needs because it then keeps one review per author.
     */
    @Override
    public List<Review> findTopRated(Rating rating, int limit) {
        return reviews.findTopByRatingAndStatusGroupedByUser(
                rating.toInt(), ReviewStatus.APPROVED,
                PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "addedAt")));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Out-of-range buckets are dropped here, which is where the knowledge that they can
     * exist belongs — a legacy document may hold the 900-star rating the S8 suite pinned, and
     * {@link Rating} cannot represent it. Dropping before the caller sums is what keeps such a
     * bucket from skewing the percentages.
     */
    @Override
    public Map<Rating, Long> ratingTally(ProductId productId) {
        Map<Rating, Long> tally = new LinkedHashMap<>();
        for (Map<String, Object> row : reviews.getRatingDistribution(productId.value())) {
            int stars = ((Number) row.get("_id")).intValue();
            if (stars < Rating.MIN || stars > Rating.MAX) {
                continue;
            }
            tally.put(Rating.of(stars), ((Number) row.get("count")).longValue());
        }
        return tally;
    }

    private void publish(Review review) {
        for (DomainEvent event : review.pullDomainEvents()) {
            events.publishEvent(event);
        }
    }
}
