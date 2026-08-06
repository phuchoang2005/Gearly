package com.dominator.gearly.catalog.application;

import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductFixture;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.reviews.domain.ReviewApproved;
import com.dominator.gearly.reviews.domain.ReviewRejected;
import com.dominator.gearly.reviews.domain.ReviewStatus;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.ReviewId;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rating rollup, now that it follows moderation rather than submission.
 *
 * <p>The arithmetic itself is {@code ProductTest}'s; what is asserted here is which event moves
 * it and which does not — the distinction the whole fix rests on.
 */
@ExtendWith(MockitoExtension.class)
class CatalogRatingListenerTest {

    @Mock private ProductRepository products;

    private CatalogRatingListener listener;

    private static final String PRODUCT_HEX = new ObjectId().toHexString();

    @BeforeEach
    void setUp() {
        listener = new CatalogRatingListener(products);
    }

    private Product productRated(int... stars) {
        return ProductFixture.aProduct().withId(PRODUCT_HEX).titled("RTX 4090").rated(stars).build();
    }

    private ReviewApproved approved(int stars, ReviewStatus from) {
        return new ReviewApproved(ReviewId.of("r1"), ProductId.of(PRODUCT_HEX),
                Rating.of(stars), from, Instant.now());
    }

    private ReviewRejected rejected(int stars, ReviewStatus from) {
        return new ReviewRejected(ReviewId.of("r1"), ProductId.of(PRODUCT_HEX),
                Rating.of(stars), from, Instant.now());
    }

    @Test
    @DisplayName("an approved review is folded into the product's rollup")
    void approvalAddsTheRating() {
        Product product = productRated(5);
        when(products.findById(ProductId.of(PRODUCT_HEX))).thenReturn(Optional.of(product));

        listener.on(approved(4, ReviewStatus.PENDING));

        assertThat(product.getRatingCount()).isEqualTo(2);
        assertThat(product.getTotalRating()).isEqualTo(9);
        assertThat(product.getAverageRating()).isEqualTo(4.5);
        verify(products).save(product);
    }

    @Test
    @DisplayName("withdrawing a published review takes its stars back")
    void withdrawalRemovesTheRating() {
        Product product = productRated(5, 4);
        when(products.findById(ProductId.of(PRODUCT_HEX))).thenReturn(Optional.of(product));

        listener.on(rejected(4, ReviewStatus.APPROVED));

        assertThat(product.getRatingCount()).isEqualTo(1);
        assertThat(product.getTotalRating()).isEqualTo(5);
        assertThat(product.getAverageRating()).isEqualTo(5.0);
        verify(products).save(product);
    }

    /**
     * The case that makes the asymmetry worth carrying {@code previousStatus} for: a review
     * refused before it was ever published never contributed, so subtracting would drag the
     * average down by a review nobody saw.
     */
    @Test
    @DisplayName("rejecting a review that was never published changes nothing")
    void rejectingAPendingReviewIsANoOp() {
        listener.on(rejected(1, ReviewStatus.PENDING));

        verify(products, never()).findById(any());
        verify(products, never()).save(any());
    }

    @Test
    @DisplayName("a product delisted since the review was written is skipped, not an error")
    void missingProductIsSkipped() {
        when(products.findById(ProductId.of(PRODUCT_HEX))).thenReturn(Optional.empty());

        listener.on(approved(5, ReviewStatus.PENDING));

        verify(products, never()).save(any());
    }
}
