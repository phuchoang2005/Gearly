package com.dominator.gearly.reviews.domain;

import com.dominator.gearly.shared.domain.Rating;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The moderation lifecycle: which moves are legal, and what each one announces.
 *
 * <p>There was nothing to test before. {@code AdminReviewService.setStatus} assigned whatever
 * constant it was handed, so every move was legal and none of them said anything.
 */
class ReviewLifecycleTest {

    @Nested
    @DisplayName("the transition table")
    class Transitions {

        @Test
        @DisplayName("a pending review can be approved or rejected")
        void pendingCanBeModerated() {
            assertThat(ReviewStatus.PENDING.canTransitionTo(ReviewStatus.APPROVED)).isTrue();
            assertThat(ReviewStatus.PENDING.canTransitionTo(ReviewStatus.REJECTED)).isTrue();
        }

        @Test
        @DisplayName("a moderator may reconsider in either direction")
        void moderationIsReversible() {
            assertThat(ReviewStatus.APPROVED.canTransitionTo(ReviewStatus.REJECTED)).isTrue();
            assertThat(ReviewStatus.REJECTED.canTransitionTo(ReviewStatus.APPROVED)).isTrue();
        }

        /**
         * The bug the plan names at {@code AdminReviewService}: nothing stopped a moderated
         * review being put back to "not yet moderated", which is not a state a decision can
         * return something to.
         */
        @Test
        @DisplayName("nothing returns to PENDING")
        void pendingIsNotATarget() {
            assertThat(ReviewStatus.APPROVED.canTransitionTo(ReviewStatus.PENDING)).isFalse();
            assertThat(ReviewStatus.REJECTED.canTransitionTo(ReviewStatus.PENDING)).isFalse();
            assertThat(ReviewStatus.PENDING.canTransitionTo(ReviewStatus.PENDING)).isFalse();
        }

        /**
         * The property the rating rollup depends on: because a status is never a legal source
         * for itself, every arrival at {@code APPROVED} is a first arrival — so
         * {@code Product.addRating} is called exactly once per review that counts, however many
         * times a moderator clicks.
         */
        @Test
        @DisplayName("no status is a legal source for itself")
        void selfTransitionsAreRefused() {
            for (ReviewStatus status : ReviewStatus.values()) {
                assertThat(status.canTransitionTo(status))
                        .as("%s -> %s", status, status)
                        .isFalse();
            }
        }

        @Test
        void onlyApprovedCountsTowardsTheRating() {
            assertThat(ReviewStatus.APPROVED.countsTowardsRating()).isTrue();
            assertThat(ReviewStatus.PENDING.countsTowardsRating()).isFalse();
            assertThat(ReviewStatus.REJECTED.countsTowardsRating()).isFalse();
        }
    }

    @Nested
    @DisplayName("the aggregate")
    class Aggregate {

        @Test
        @DisplayName("a submitted review starts PENDING and announces nothing")
        void submitAnnouncesNothing() {
            Review review = ReviewFixture.aReview().rated(5).build();

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);
            assertThat(review.isPublished()).isFalse();
            // The rollup used to happen here, while the review was still unmoderated.
            assertThat(review.pullDomainEvents()).isEmpty();
        }

        @Test
        void approvingPublishesAndAnnouncesTheRating() {
            Review review = ReviewFixture.aReview().withId("r1").rated(4).build();

            review.approve();

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.APPROVED);
            assertThat(review.pullDomainEvents()).singleElement()
                    .isInstanceOfSatisfying(ReviewApproved.class, event -> {
                        assertThat(event.rating()).isEqualTo(Rating.of(4));
                        assertThat(event.productId()).isEqualTo(review.getProductId());
                        assertThat(event.previousStatus()).isEqualTo(ReviewStatus.PENDING);
                    });
        }

        @Test
        @DisplayName("rejecting a pending review announces a rejection that withdraws nothing")
        void rejectingAPendingReviewWithdrawsNothing() {
            Review review = ReviewFixture.aReview().withId("r1").build();

            review.reject();

            assertThat(review.pullDomainEvents()).singleElement()
                    .isInstanceOfSatisfying(ReviewRejected.class, event -> {
                        assertThat(event.previousStatus()).isEqualTo(ReviewStatus.PENDING);
                        assertThat(event.withdrawsACountedRating()).isFalse();
                    });
        }

        @Test
        @DisplayName("withdrawing an approved review announces a rejection that does take stars back")
        void rejectingAnApprovedReviewWithdrawsTheRating() {
            Review review = ReviewFixture.aReview().withId("r1").rated(2).approved().build();

            review.reject();

            assertThat(review.pullDomainEvents()).singleElement()
                    .isInstanceOfSatisfying(ReviewRejected.class, event -> {
                        assertThat(event.previousStatus()).isEqualTo(ReviewStatus.APPROVED);
                        assertThat(event.withdrawsACountedRating()).isTrue();
                        assertThat(event.rating()).isEqualTo(Rating.of(2));
                    });
        }

        @Test
        @DisplayName("approving twice is refused, and announces nothing the second time")
        void doubleApprovalIsRefused() {
            Review review = ReviewFixture.aReview().withId("r1").approved().build();

            assertThatThrownBy(review::approve)
                    .isInstanceOf(IllegalReviewTransitionException.class);
            assertThat(review.pullDomainEvents()).isEmpty();
        }

        @Test
        void rejectingTwiceIsRefused() {
            Review review = ReviewFixture.aReview().withId("r1").rejected().build();

            assertThatThrownBy(review::reject)
                    .isInstanceOf(IllegalReviewTransitionException.class);
            assertThat(review.pullDomainEvents()).isEmpty();
        }

        @Test
        @DisplayName("a review refused in error can be reinstated, and counts again")
        void aRejectedReviewCanBeApproved() {
            Review review = ReviewFixture.aReview().withId("r1").rated(5).rejected().build();

            review.approve();

            assertThat(review.getStatus()).isEqualTo(ReviewStatus.APPROVED);
            assertThat(review.pullDomainEvents()).singleElement()
                    .isInstanceOfSatisfying(ReviewApproved.class, event ->
                            assertThat(event.previousStatus()).isEqualTo(ReviewStatus.REJECTED));
        }

        /**
         * S9 built {@code Rating} and left this field an {@code int} so a legacy document
         * holding the 900 stars the S8 suite pinned would stay readable. S12 adopts it, and
         * {@code migrate.js} step 9 clamps those documents first.
         */
        @Test
        @DisplayName("a rating outside 1-5 cannot be submitted at all")
        void outOfRangeRatingIsUnrepresentable() {
            assertThatThrownBy(() -> ReviewFixture.aReview().rated(900).build())
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> ReviewFixture.aReview().rated(0).build())
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
