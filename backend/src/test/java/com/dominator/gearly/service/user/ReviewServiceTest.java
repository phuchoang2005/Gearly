package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.CreateReviewRequestDTO;
import com.dominator.gearly.dto.CreateReviewsRequestDTO;
import com.dominator.gearly.dto.ProductReviewsDTO;
import com.dominator.gearly.dto.ReviewRatingDTO;
import com.dominator.gearly.dto.ReviewResponseDTO;
import com.dominator.gearly.exception.ApiException;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.mapper.ReviewMapper;
import com.dominator.gearly.model.Order;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.Review;
import com.dominator.gearly.model.ReviewStatus;
import com.dominator.gearly.model.User;
import com.dominator.gearly.repository.OrderRepository;
import com.dominator.gearly.repository.ProductRepository;
import com.dominator.gearly.repository.ReviewRepository;
import com.dominator.gearly.repository.UserRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.UserId;
import com.dominator.gearly.shared.domain.OrderId;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <b>Characterization suite (S8).</b> Locks the <i>current</i> behavior of
 * {@link ReviewService} — bugs included — ahead of the S12 rewrite into a {@code reviews}
 * context. The rating roll-up is the critical part: S12 moves it onto
 * {@code Product.addRating(...)} driven by a {@code ReviewApproved} event, and every
 * assertion here that pins the current create-time roll-up is marked as such.
 */
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;

    private ReviewService service;

    private static final String USER_ID = new ObjectId().toHexString();
    private static final String ORDER_ID = new ObjectId().toHexString();
    private static final String PRODUCT_ID = new ObjectId().toHexString();

    @BeforeEach
    void setUp() {
        service = new ReviewService(reviewRepository, userRepository, orderRepository,
                productRepository, new ReviewMapper());
    }

    // ---- fixtures ----------------------------------------------------------

    private AuthenticatedUser authUser(String userId) {
        User user = new User();
        user.setId(userId);
        return new AuthenticatedUser(user);
    }

    private Product product(int ratingCount, int totalRating) {
        Product p = new Product();
        p.setId(PRODUCT_ID);
        p.setTitle("Product");
        p.setRatingCount(ratingCount);
        p.setTotalRating(totalRating);
        return p;
    }

    private Order ownedOrder(OrderStatus status, boolean reviewed) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUserId(USER_ID);
        order.setOrderStatus(status);
        order.setReviewed(reviewed);
        return order;
    }

    private CreateReviewsRequestDTO reviewRequest(int rating) {
        return new CreateReviewsRequestDTO(ORDER_ID, List.of(
                new CreateReviewRequestDTO(PRODUCT_ID, "Great", "Works well", rating)));
    }

    @SuppressWarnings("unchecked")
    private List<Product> captureSavedProducts() {
        ArgumentCaptor<List<Product>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @SuppressWarnings("unchecked")
    private List<Review> captureSavedReviews() {
        ArgumentCaptor<List<Review>> captor = ArgumentCaptor.forClass(List.class);
        verify(reviewRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    // ---- createReview: the rating roll-up ----------------------------------

    @Nested
    @DisplayName("createReview — rating roll-up")
    class RatingRollup {

        @Test
        @DisplayName("the first review sets count 1, total, and the average")
        void firstReviewSeedsTheAverage() {
            Product p = product(0, 0);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(p));

            service.createReview(authUser(USER_ID), reviewRequest(5));

            assertThat(captureSavedProducts()).singleElement().satisfies(saved -> {
                assertThat(saved.getRatingCount()).isEqualTo(1);
                assertThat(saved.getTotalRating()).isEqualTo(5);
                assertThat(saved.getAverageRating()).isEqualTo(5.0);
            });
        }

        @Test
        @DisplayName("a subsequent review folds into the running total")
        void subsequentReviewUpdatesTheAverage() {
            Product p = product(1, 5);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(p));

            service.createReview(authUser(USER_ID), reviewRequest(4));

            assertThat(captureSavedProducts()).singleElement().satisfies(saved -> {
                assertThat(saved.getRatingCount()).isEqualTo(2);
                assertThat(saved.getTotalRating()).isEqualTo(9);
                assertThat(saved.getAverageRating()).isEqualTo(4.5);
            });
        }

        @Test
        @DisplayName("the average is truncated to two decimals via Math.round")
        void averageIsRoundedToTwoDecimals() {
            Product p = product(2, 9); // 9 + 4 = 13 over 3 -> 4.3333...
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(p));

            service.createReview(authUser(USER_ID), reviewRequest(4));

            assertThat(captureSavedProducts()).singleElement()
                    .extracting(Product::getAverageRating)
                    .isEqualTo(4.33);
        }

        @Test
        @DisplayName("KNOWN BUG: the rating is unbounded — 900 stars is accepted and wrecks the average")
        void unboundedRatingIsAccepted() {
            // CreateReviewRequestDTO has no @Min/@Max and applyRating does no validation.
            // S9's Rating value object (1..5) closes this; this assertion changes then.
            Product p = product(0, 0);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(p));

            service.createReview(authUser(USER_ID), reviewRequest(900));

            assertThat(captureSavedProducts()).singleElement()
                    .extracting(Product::getAverageRating)
                    .isEqualTo(900.0);
        }

        @Test
        @DisplayName("KNOWN BUG: a rating of 0 is accepted and drags the average below the 1-star floor")
        void zeroRatingIsAccepted() {
            Product p = product(1, 5);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(p));

            service.createReview(authUser(USER_ID), reviewRequest(0));

            assertThat(captureSavedProducts()).singleElement()
                    .extracting(Product::getAverageRating)
                    .isEqualTo(2.5);
        }

        @Test
        @DisplayName("KNOWN INCONSISTENCY: the roll-up happens at creation, while the review is still PENDING")
        void ratingCountsBeforeModeration() {
            // The public distribution query filters status:'APPROVED', so averageRating and the
            // star histogram are structurally inconsistent. S12 moves the roll-up to a
            // ReviewApproved event and adds a recompute migration.
            Product p = product(0, 0);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(p));

            service.createReview(authUser(USER_ID), reviewRequest(5));

            assertThat(captureSavedReviews()).singleElement()
                    .extracting(Review::getStatus)
                    .isEqualTo(ReviewStatus.PENDING);
            assertThat(captureSavedProducts()).singleElement()
                    .extracting(Product::getRatingCount)
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("KNOWN BUG: reviewing the same order twice re-inflates the roll-up")
        void doubleReviewReInflatesTheRollup() {
            // createReview writes order.reviewed = true but never reads it back, so a repeated
            // call counts again. S12 adds the isReviewed guard.
            Product p = product(0, 0);
            when(orderRepository.findById(ORDER_ID))
                    .thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)))
                    .thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, true)));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(p));

            service.createReview(authUser(USER_ID), reviewRequest(5));
            service.createReview(authUser(USER_ID), reviewRequest(5));

            assertThat(p.getRatingCount()).isEqualTo(2);
            assertThat(p.getTotalRating()).isEqualTo(10);
        }

        @Test
        @DisplayName("KNOWN BUG: a CANCELLED order is reviewable — no order status is required")
        void cancelledOrderIsReviewable() {
            // S12 adds a reviewable-status rule.
            Product p = product(0, 0);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.CANCELLED, false)));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(p));

            service.createReview(authUser(USER_ID), reviewRequest(5));

            verify(reviewRepository).saveAll(anyList());
        }
    }

    // ---- createReview: persistence + guards --------------------------------

    @Nested
    @DisplayName("createReview — persistence and guards")
    class Persistence {

        @Test
        @DisplayName("the review carries the product, order and user ids as typed ids and starts PENDING")
        void reviewIsBuiltFromTheRequest() {
            Product p = product(0, 0);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(p));

            service.createReview(authUser(USER_ID), reviewRequest(5));

            assertThat(captureSavedReviews()).singleElement().satisfies(review -> {
                assertThat(review.getProductId()).isEqualTo(ProductId.of(PRODUCT_ID));
                assertThat(review.getOrderId()).isEqualTo(OrderId.of(ORDER_ID));
                assertThat(review.getUserId()).isEqualTo(UserId.of(USER_ID));
                assertThat(review.getRating()).isEqualTo(5);
                assertThat(review.getSubject()).isEqualTo("Great");
                assertThat(review.getComment()).isEqualTo("Works well");
                assertThat(review.getStatus()).isEqualTo(ReviewStatus.PENDING);
            });
        }

        @Test
        @DisplayName("the order is flagged as reviewed and saved")
        void orderIsFlaggedReviewed() {
            Product p = product(0, 0);
            Order order = ownedOrder(OrderStatus.DELIVERED, false);
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of(p));

            service.createReview(authUser(USER_ID), reviewRequest(5));

            assertThat(order.isReviewed()).isTrue();
            verify(orderRepository).save(order);
        }

        @Test
        @DisplayName("an unknown order is a 404")
        void unknownOrderThrows() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.createReview(authUser(USER_ID), reviewRequest(5)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order not found, you cannot create review on this.");
        }

        @Test
        @DisplayName("reviewing someone else's order is forbidden")
        void otherUsersOrderIsForbidden() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)));

            String otherUser = new ObjectId().toHexString();
            assertThatThrownBy(() -> service.createReview(authUser(otherUser), reviewRequest(5)))
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getStatus())
                    .isEqualTo(HttpStatus.FORBIDDEN);

            verify(reviewRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("a review for a product that no longer exists is a 404, and nothing is written")
        void unknownProductThrowsAndWritesNothing() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)));
            when(productRepository.findAllById(List.of(PRODUCT_ID))).thenReturn(List.of());

            assertThatThrownBy(() -> service.createReview(authUser(USER_ID), reviewRequest(5)))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product not found, you cannot create review for this product.");

            verify(reviewRepository, never()).saveAll(anyList());
            verify(orderRepository, never()).save(any());
        }

        @Test
        @DisplayName("the order-ownership check happens before any product is loaded")
        void ownershipIsCheckedFirst() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(ownedOrder(OrderStatus.DELIVERED, false)));

            String otherUser = new ObjectId().toHexString();
            assertThatThrownBy(() -> service.createReview(authUser(otherUser), reviewRequest(5)))
                    .isInstanceOf(ApiException.class);

            verify(productRepository, never()).findAllById(anyList());
        }
    }

    // ---- read paths --------------------------------------------------------

    @Nested
    @DisplayName("read paths")
    class Reads {

        private Review approvedReview(String id, String userId, int rating) {
            Review review = new Review();
            review.setId(id);
            review.setUserId(UserId.of(userId));
            review.setProductId(ProductId.of(PRODUCT_ID));
            review.setRating(rating);
            review.setSubject("s-" + id);
            review.setComment("c-" + id);
            review.setStatus(ReviewStatus.APPROVED);
            review.setAddedAt(Instant.parse("2026-01-01T00:00:00Z"));
            return review;
        }

        private User namedUser(String id, String fullName) {
            User user = new User();
            user.setId(id);
            user.setFullName(fullName);
            return user;
        }

        @Test
        @DisplayName("a rating filter of 0 means 'any rating'")
        void ratingZeroMeansUnfiltered() {
            ProductReviewsDTO dto = new ProductReviewsDTO(PRODUCT_ID, 10, 0, 0, "addedAt");
            when(reviewRepository.findByProductIdAndStatus(
                    eq(new ObjectId(PRODUCT_ID)), eq(ReviewStatus.APPROVED), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(approvedReview("r1", USER_ID, 5))));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(namedUser(USER_ID, "Ada Lovelace")));

            Page<ReviewResponseDTO> page = service.getApprovedReviews(dto);

            assertThat(page.getContent()).singleElement().satisfies(review -> {
                assertThat(review.getId()).isEqualTo("r1");
                assertThat(review.getRating()).isEqualTo(5);
                assertThat(review.getUserName()).isEqualTo("Ada Lovelace");
            });
        }

        @Test
        @DisplayName("a non-zero rating filter goes to the rating-scoped query")
        void nonZeroRatingFilters() {
            ProductReviewsDTO dto = new ProductReviewsDTO(PRODUCT_ID, 10, 0, 4, "addedAt");
            when(reviewRepository.findByProductIdAndStatusAndRating(
                    eq(new ObjectId(PRODUCT_ID)), eq(ReviewStatus.APPROVED), eq(4), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of()));

            assertThat(service.getApprovedReviews(dto)).isEmpty();
            verify(reviewRepository, never())
                    .findByProductIdAndStatus(any(), any(), any());
        }

        @Test
        @DisplayName("a review whose author was deleted renders as 'Unknown User'")
        void missingAuthorFallsBackToUnknown() {
            ProductReviewsDTO dto = new ProductReviewsDTO(PRODUCT_ID, 10, 0, 0, "addedAt");
            when(reviewRepository.findByProductIdAndStatus(any(), any(), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(approvedReview("r1", USER_ID, 5))));
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThat(service.getApprovedReviews(dto).getContent())
                    .singleElement()
                    .extracting(ReviewResponseDTO::getUserName)
                    .isEqualTo("Unknown User");
        }

        @Test
        @DisplayName("getSixBestReviews keeps only the first review per author and caps the list at six")
        void sixBestDeduplicatesByAuthor() {
            String userA = new ObjectId().toHexString();
            String userB = new ObjectId().toHexString();
            when(reviewRepository.findTopByRatingAndStatusGroupedByUser(
                    eq(5), eq(ReviewStatus.APPROVED), any(Pageable.class)))
                    .thenReturn(List.of(
                            approvedReview("r1", userA, 5),
                            approvedReview("r2", userA, 5),
                            approvedReview("r3", userB, 5)));
            when(userRepository.findById(userA)).thenReturn(Optional.of(namedUser(userA, "A")));
            when(userRepository.findById(userB)).thenReturn(Optional.of(namedUser(userB, "B")));

            List<ReviewResponseDTO> result = service.getSixBestReviews();

            assertThat(result).extracting(ReviewResponseDTO::getId, ReviewResponseDTO::getUserName)
                    .containsExactly(tuple("r1", "A"), tuple("r3", "B"));
        }

        @Test
        @DisplayName("the distribution runs 5 stars down to 1, with percentages rounded to whole numbers")
        void distributionIsFiveDownToOne() {
            when(reviewRepository.getRatingDistribution(new ObjectId(PRODUCT_ID)))
                    .thenReturn(List.of(row(5, 2L), row(3, 1L)));

            List<ReviewRatingDTO> distribution = service.getRatingDistribution(PRODUCT_ID);

            assertThat(distribution).extracting(ReviewRatingDTO::getStars, ReviewRatingDTO::getCount,
                            ReviewRatingDTO::getPercentage)
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
            when(reviewRepository.getRatingDistribution(new ObjectId(PRODUCT_ID))).thenReturn(List.of());

            assertThat(service.getRatingDistribution(PRODUCT_ID))
                    .hasSize(5)
                    .allSatisfy(entry -> {
                        assertThat(entry.getCount()).isZero();
                        assertThat(entry.getPercentage()).isZero();
                    });
        }

        @Test
        @DisplayName("out-of-range star buckets in the aggregation result are ignored")
        void outOfRangeBucketsAreIgnored() {
            when(reviewRepository.getRatingDistribution(new ObjectId(PRODUCT_ID)))
                    .thenReturn(List.of(row(5, 1L), row(900, 3L)));

            List<ReviewRatingDTO> distribution = service.getRatingDistribution(PRODUCT_ID);

            // The 900-star bucket is dropped before the total is summed, so it does not skew the
            // percentages — the 5-star share reads 100%.
            assertThat(distribution).extracting(ReviewRatingDTO::getStars, ReviewRatingDTO::getCount)
                    .containsExactly(tuple(5, 1L), tuple(4, 0L), tuple(3, 0L), tuple(2, 0L), tuple(1, 0L));
            assertThat(distribution.getFirst().getPercentage()).isEqualTo(100.0);
        }

        private Map<String, Object> row(int stars, long count) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("_id", stars);
            row.put("count", count);
            return row;
        }
    }
}
