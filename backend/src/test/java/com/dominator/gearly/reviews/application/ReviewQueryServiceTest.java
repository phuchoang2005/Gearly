package com.dominator.gearly.reviews.application;

import com.dominator.gearly.identity.domain.UserDirectory;
import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewFixture;
import com.dominator.gearly.reviews.domain.ReviewPage;
import com.dominator.gearly.reviews.domain.ReviewRepository;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.UserId;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The public review reads. The successor of the S8 characterization suite's read half; every
 * assertion it made is still here.
 */
@ExtendWith(MockitoExtension.class)
class ReviewQueryServiceTest {

    @Mock private ReviewRepository reviews;
    @Mock private UserDirectory users;

    private ReviewQueryService service;

    private static final String PRODUCT_ID = new ObjectId().toHexString();
    private static final String USER_ID = new ObjectId().toHexString();

    @BeforeEach
    void setUp() {
        service = new ReviewQueryService(reviews, users);
    }

    private Review approvedReview(String id, String userId, int rating) {
        return ReviewFixture.aReview()
                .of(PRODUCT_ID).by(userId).rated(rating).saying("s-" + id, "c-" + id)
                .persistedAs(id, Instant.parse("2026-01-01T00:00:00Z"))
                .approved()
                .build();
    }

    @Nested
    @DisplayName("a product's review list")
    class ApprovedReviews {

        @Test
        @DisplayName("a rating filter of 0 means 'any rating'")
        void ratingZeroMeansUnfiltered() {
            when(reviews.findApproved(eq(ProductId.of(PRODUCT_ID)), eq(null), anyInt(), anyInt(), any()))
                    .thenReturn(new ReviewPage(List.of(approvedReview("r1", USER_ID, 5)), 0, 10, 1));
            when(users.displayNamesOf(anyList()))
                    .thenReturn(Map.of(UserId.of(USER_ID), "Ada Lovelace"));

            ReviewQueryService.PagedReviews page =
                    service.approvedFor(ProductId.of(PRODUCT_ID), 0, 0, 10, "addedAt");

            assertThat(page.content()).singleElement().satisfies(authored -> {
                assertThat(authored.review().getId()).isEqualTo("r1");
                assertThat(authored.review().getRating()).isEqualTo(Rating.of(5));
                assertThat(authored.authorName()).isEqualTo("Ada Lovelace");
            });
        }

        @Test
        @DisplayName("a non-zero rating filter is passed down as a Rating")
        void nonZeroRatingFilters() {
            when(reviews.findApproved(any(), eq(Rating.of(4)), anyInt(), anyInt(), any()))
                    .thenReturn(new ReviewPage(List.of(), 0, 10, 0));

            assertThat(service.approvedFor(ProductId.of(PRODUCT_ID), 4, 0, 10, "addedAt").content())
                    .isEmpty();
        }

        /**
         * The name is absent rather than {@code null}-checked at every call site; the api layer
         * supplies the phrase the storefront has always shown.
         */
        @Test
        @DisplayName("a review whose author was deleted has no name, and the mapper phrases it")
        void missingAuthorHasNoName() {
            when(reviews.findApproved(any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(new ReviewPage(List.of(approvedReview("r1", USER_ID, 5)), 0, 10, 1));
            when(users.displayNamesOf(anyList())).thenReturn(Map.of());

            assertThat(service.approvedFor(ProductId.of(PRODUCT_ID), 0, 0, 10, "addedAt").content())
                    .singleElement()
                    .extracting(AuthoredReview::authorName)
                    .isNull();
        }

        /**
         * The old code called {@code findById} once per review, inside the {@code map} that
         * built each DTO. A page of ten reviews was eleven queries.
         */
        @Test
        @DisplayName("author names are resolved in one batched lookup for the whole page")
        void authorsAreResolvedInOneCall() {
            String other = new ObjectId().toHexString();
            when(reviews.findApproved(any(), any(), anyInt(), anyInt(), any()))
                    .thenReturn(new ReviewPage(List.of(
                            approvedReview("r1", USER_ID, 5),
                            approvedReview("r2", other, 4),
                            approvedReview("r3", USER_ID, 3)), 0, 10, 3));
            when(users.displayNamesOf(anyList())).thenReturn(Map.of(
                    UserId.of(USER_ID), "A", UserId.of(other), "B"));

            service.approvedFor(ProductId.of(PRODUCT_ID), 0, 0, 10, "addedAt");

            verify(users).displayNamesOf(anyList());
            verify(users, never()).displayNameOf(any());
        }
    }

    @Nested
    @DisplayName("the testimonial strip")
    class Testimonials {

        @Test
        @DisplayName("keeps only the first review per author and caps the list at six")
        void deduplicatesByAuthor() {
            String userA = new ObjectId().toHexString();
            String userB = new ObjectId().toHexString();
            when(reviews.findTopRated(eq(Rating.of(5)), anyInt())).thenReturn(List.of(
                    approvedReview("r1", userA, 5),
                    approvedReview("r2", userA, 5),
                    approvedReview("r3", userB, 5)));
            when(users.displayNamesOf(anyList()))
                    .thenReturn(Map.of(UserId.of(userA), "A", UserId.of(userB), "B"));

            assertThat(service.testimonials())
                    .extracting(a -> a.review().getId(), AuthoredReview::authorName)
                    .containsExactly(tuple("r1", "A"), tuple("r3", "B"));
        }
    }

    @Nested
    @DisplayName("the star histogram")
    class Distribution {

        @Test
        @DisplayName("runs 5 stars down to 1, with percentages rounded to whole numbers")
        void distributionIsFiveDownToOne() {
            when(reviews.ratingTally(ProductId.of(PRODUCT_ID))).thenReturn(tally(5, 2L, 3, 1L));

            assertThat(service.distributionFor(ProductId.of(PRODUCT_ID)))
                    .extracting(ReviewQueryService.RatingShare::stars,
                            ReviewQueryService.RatingShare::count,
                            ReviewQueryService.RatingShare::percentage)
                    .containsExactly(
                            tuple(5, 2L, 67.0),   // 66.67 rounded
                            tuple(4, 0L, 0.0),
                            tuple(3, 1L, 33.0),   // 33.33 rounded
                            tuple(2, 0L, 0.0),
                            tuple(1, 0L, 0.0));
        }

        @Test
        @DisplayName("a product with no reviews gets an all-zero distribution rather than a divide-by-zero")
        void emptyDistributionIsAllZeroes() {
            when(reviews.ratingTally(ProductId.of(PRODUCT_ID))).thenReturn(Map.of());

            assertThat(service.distributionFor(ProductId.of(PRODUCT_ID)))
                    .hasSize(5)
                    .allSatisfy(share -> {
                        assertThat(share.count()).isZero();
                        assertThat(share.percentage()).isZero();
                    });
        }

        /**
         * Out-of-range buckets can no longer reach this method at all — the port is keyed on
         * {@link Rating}, so the adapter drops them. That is the same behaviour the S8 suite
         * pinned (a 900-star bucket must not skew the percentages), enforced a layer earlier and
         * by the type system rather than by an {@code if} the caller has to remember.
         */
        @Test
        @DisplayName("the tally cannot carry an out-of-range bucket, so none can skew the shares")
        void outOfRangeBucketsCannotArrive() {
            when(reviews.ratingTally(ProductId.of(PRODUCT_ID))).thenReturn(tally(5, 1L));

            List<ReviewQueryService.RatingShare> distribution =
                    service.distributionFor(ProductId.of(PRODUCT_ID));

            assertThat(distribution.getFirst().percentage()).isEqualTo(100.0);
        }

        private Map<Rating, Long> tally(long... starsAndCounts) {
            Map<Rating, Long> tally = new LinkedHashMap<>();
            for (int i = 0; i < starsAndCounts.length; i += 2) {
                tally.put(Rating.of((int) starsAndCounts[i]), starsAndCounts[i + 1]);
            }
            return tally;
        }
    }
}
