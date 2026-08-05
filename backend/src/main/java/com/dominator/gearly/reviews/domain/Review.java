package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.AggregateRoot;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.ReviewId;
import com.dominator.gearly.shared.domain.UserId;
import com.dominator.gearly.shared.infrastructure.ObjectIdBackedIdConverters;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.convert.ValueConverter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Objects;

/**
 * <b>The reviews context's aggregate root.</b> One customer's opinion of one product they
 * bought, and where that opinion is in moderation.
 *
 * <h2>What changed</h2>
 * This was a Lombok {@code @Getter @Setter} bag with no behavior at all, and the consequences
 * were the two the plan lists:
 *
 * <ul>
 *   <li><b>Moderation had no rule.</b> {@code AdminReviewService.setStatus} assigned whatever
 *       constant it was handed, so any status could follow any other. It is {@link #approve()}
 *       and {@link #reject()} now, guarded by {@link ReviewStatus}'s transition table.</li>
 *   <li><b>The rating was an unbounded {@code int}.</b> S9 built {@link Rating} and
 *       deliberately left the field alone, because a legacy document carrying the 900 stars
 *       the S8 suite pinned had to stay readable until something owned the reviews context.
 *       Something does now, so the field is a {@code Rating} — and
 *       {@code data/seed/migrate.js} clamps any stored value outside 1–5 first, because after
 *       this change such a document does not deserialize at all.</li>
 * </ul>
 *
 * <h2>The rollup moved off this class's creation path</h2>
 * Submitting a review no longer touches the product. {@link #approve()} raises
 * {@link ReviewApproved} and {@link #reject()} raises {@link ReviewRejected}; the catalog
 * listens and adjusts. See {@link ReviewApproved} for why that inconsistency was structural
 * rather than a bug in the arithmetic.
 *
 * <h2>Persistence</h2>
 * Still a {@code @Document}, and the stored shape is byte-identical: {@code rating} writes as a
 * BSON {@code int32} through the S9 converter, and the three id fields keep the
 * {@code ObjectId} form the rating-distribution aggregation joins on — see
 * {@link ObjectIdBackedIdConverters} for why those three are per-property converters rather
 * than global ones.
 */
@Getter
@Document(collection = "reviews")
public class Review extends AggregateRoot {

    @Id
    private String id;

    /**
     * These three are the id asymmetry the shared kernel absorbs: they are stored as BSON
     * {@code ObjectId} while the same id types are stored as plain strings everywhere else
     * (an order's {@code items[].productId}, a cart's line, {@code Order.userId}). The
     * {@code @ValueConverter}s keep the stored form exactly as it is.
     */
    @ValueConverter(ObjectIdBackedIdConverters.ProductIdAsObjectId.class)
    private ProductId productId;

    @ValueConverter(ObjectIdBackedIdConverters.OrderIdAsObjectId.class)
    private OrderId orderId;

    @ValueConverter(ObjectIdBackedIdConverters.UserIdAsObjectId.class)
    private UserId userId;

    /** 1–5 by construction. See the class note for why this was an {@code int} until S12. */
    private Rating rating;

    private String subject;
    private String comment;

    private ReviewStatus status = ReviewStatus.PENDING;

    /**
     * Real BSON dates since S9. {@code getSixBestReviews} sorts on {@code addedAt}; as a string
     * that was a lexicographic sort that only coincided with chronological order because every
     * stored value happened to share one format.
     */
    @CreatedDate
    private Instant addedAt;

    @LastModifiedDate
    private Instant modifiedAt;

    /** For Spring Data. */
    protected Review() {
    }

    // ------------------------------------------------------------------------
    // Creation
    // ------------------------------------------------------------------------

    /**
     * A customer writes a review of something they bought. Opens {@code PENDING}, and — unlike
     * before — changes nothing about the product until a moderator publishes it.
     *
     * <p>Whether this customer is entitled to review this order is not asked here: it is a fact
     * about the order, and ordering answers it through {@code ReviewableOrders}. The aggregate
     * takes the ids it is given.
     */
    public static Review submit(ProductId productId,
                                OrderId orderId,
                                UserId userId,
                                Rating rating,
                                String subject,
                                String comment) {
        Review review = new Review();
        review.productId = Objects.requireNonNull(productId, "a review is of a product");
        review.orderId = Objects.requireNonNull(orderId, "a review follows an order");
        review.userId = Objects.requireNonNull(userId, "a review has an author");
        review.rating = Objects.requireNonNull(rating, "a review carries a rating");
        review.subject = subject;
        review.comment = comment;
        review.status = ReviewStatus.PENDING;
        return review;
    }

    // ------------------------------------------------------------------------
    // Reads
    // ------------------------------------------------------------------------

    /** The typed identity. Null until Mongo has assigned one on first save. */
    public ReviewId reviewId() {
        return id == null ? null : ReviewId.of(id);
    }

    public boolean isAuthoredBy(UserId candidate) {
        return userId != null && userId.equals(candidate);
    }

    public boolean isPublished() {
        return status.countsTowardsRating();
    }

    // ------------------------------------------------------------------------
    // Moderation
    // ------------------------------------------------------------------------

    /**
     * Publish the review, and tell the catalog to count it.
     *
     * @throws IllegalReviewTransitionException if it is already approved — which is what makes
     *         the rollup safe, since every arrival here is therefore a first arrival
     */
    public void approve() {
        ReviewStatus previous = status;
        status.assertCanTransitionTo(ReviewStatus.APPROVED);
        status = ReviewStatus.APPROVED;
        touch();
        registerEvent(new ReviewApproved(reviewId(), productId, rating, previous, Instant.now()));
    }

    /**
     * Refuse the review, or withdraw one already published.
     *
     * <p>The event goes out either way; whether the catalog has a rating to take back is
     * decided by the status it is coming from. See {@link ReviewRejected}.
     */
    public void reject() {
        ReviewStatus previous = status;
        status.assertCanTransitionTo(ReviewStatus.REJECTED);
        status = ReviewStatus.REJECTED;
        touch();
        registerEvent(new ReviewRejected(reviewId(), productId, rating, previous, Instant.now()));
    }

    /**
     * Stamps {@code modifiedAt}. {@code @LastModifiedDate} would do this on save anyway; the
     * aggregate does it at the moment the change happens, as {@code Order} and {@code Product}
     * do, so a test can observe the change before it is persisted.
     */
    private void touch() {
        this.modifiedAt = Instant.now();
    }
}
