package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.UserId;
import org.bson.types.ObjectId;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

/**
 * Builds {@link Review} aggregates for tests, under the same two rules as {@code ProductFixture},
 * {@code OrderFixture} and {@code UserFixture}:
 *
 * <ol>
 *   <li><b>State is reached through real behavior.</b> {@link Builder#approved()} calls
 *       {@code approve()}, so a fixture cannot describe a review the lifecycle could not
 *       produce — it cannot, for instance, invent an {@code APPROVED} review that never raised
 *       {@code ReviewApproved}.</li>
 *   <li><b>Reflection touches only the persistence-managed fields</b> — {@code id} and the two
 *       audit timestamps, which Spring Data populates on load and nothing else does.</li>
 * </ol>
 *
 * <p>{@code build()} drains whatever the moderation raised, so a fixture standing for "a review
 * that is already approved" does not announce itself again the moment a test saves it.
 */
public final class ReviewFixture {

    private ReviewFixture() {
    }

    public static Builder aReview() {
        return new Builder();
    }

    public static final class Builder {

        private String id;
        private String productId = new ObjectId().toHexString();
        private String orderId = new ObjectId().toHexString();
        private String userId = new ObjectId().toHexString();
        private int rating = 5;
        private String subject = "Great";
        private String comment = "Works well";
        private ReviewStatus moderateTo;
        private Instant addedAt;

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder of(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder from(String orderId) {
            this.orderId = orderId;
            return this;
        }

        public Builder by(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder rated(int rating) {
            this.rating = rating;
            return this;
        }

        public Builder saying(String subject, String comment) {
            this.subject = subject;
            this.comment = comment;
            return this;
        }

        public Builder approved() {
            this.moderateTo = ReviewStatus.APPROVED;
            return this;
        }

        public Builder rejected() {
            this.moderateTo = ReviewStatus.REJECTED;
            return this;
        }

        public Builder persistedAs(String id, Instant addedAt) {
            this.id = id;
            this.addedAt = addedAt;
            return this;
        }

        public Review build() {
            Review review = Review.submit(
                    ProductId.of(productId),
                    OrderId.of(orderId),
                    UserId.of(userId),
                    Rating.of(rating),
                    subject,
                    comment);

            setPersistenceField(review, "id", id);
            setPersistenceField(review, "addedAt", addedAt);
            setPersistenceField(review, "modifiedAt", addedAt);

            if (moderateTo == ReviewStatus.APPROVED) {
                review.approve();
            } else if (moderateTo == ReviewStatus.REJECTED) {
                review.reject();
            }
            review.pullDomainEvents();
            return review;
        }

        private void setPersistenceField(Review review, String field, Object value) {
            if (value != null) {
                ReflectionTestUtils.setField(review, field, value);
            }
        }
    }
}
