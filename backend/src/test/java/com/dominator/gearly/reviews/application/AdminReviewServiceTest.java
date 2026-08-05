package com.dominator.gearly.reviews.application;

import com.dominator.gearly.catalog.domain.CatalogSnapshot;
import com.dominator.gearly.catalog.domain.ProductSnapshotPort;
import com.dominator.gearly.identity.domain.UserDirectory;
import com.dominator.gearly.reviews.domain.IllegalReviewTransitionException;
import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewFixture;
import com.dominator.gearly.reviews.domain.ReviewNotFoundException;
import com.dominator.gearly.reviews.domain.ReviewRepository;
import com.dominator.gearly.reviews.domain.ReviewStatus;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.ReviewId;
import com.dominator.gearly.shared.domain.UserId;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Moderation. Carries forward the S7 assertions (the console gets a resolved product title and
 * author name rather than raw ids) and adds what S12 introduced: a lifecycle that refuses a
 * decision already taken.
 */
@ExtendWith(MockitoExtension.class)
class AdminReviewServiceTest {

    @Mock private ReviewRepository reviews;
    @Mock private ProductSnapshotPort catalog;
    @Mock private UserDirectory users;

    private AdminReviewService service;

    private final String productHex = new ObjectId().toHexString();
    private final String userHex = new ObjectId().toHexString();

    @BeforeEach
    void setUp() {
        service = new AdminReviewService(reviews, catalog, users);
    }

    /** What the catalog publishes — all this screen needs from it is the title. */
    private CatalogSnapshot snapshot(String title) {
        return new CatalogSnapshot(ProductId.of(productHex), title, "NVIDIA", Money.of(1599.0),
                "http://img/gpu.png", ProductCondition.NEW, Quantity.of(5));
    }

    private Review pendingReview() {
        return ReviewFixture.aReview().withId("r1").of(productHex).by(userHex).rated(5)
                .saying("Great", "Runs cool").build();
    }

    @Test
    void approveReview_setsApproved_andResolvesTitleAndName() {
        when(reviews.findById(ReviewId.of("r1"))).thenReturn(Optional.of(pendingReview()));
        when(reviews.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(catalog.findSnapshot(ProductId.of(productHex)))
                .thenReturn(Optional.of(snapshot("RTX 4090")));
        when(users.displayNameOf(UserId.of(userHex))).thenReturn(Optional.of("Alice Nguyen"));

        AdminReviewService.ModeratedReview moderated = service.approveReview("r1");

        assertThat(moderated.review().getId()).isEqualTo("r1");
        assertThat(moderated.review().getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(moderated.productTitle()).isEqualTo("RTX 4090");
        assertThat(moderated.authorName()).isEqualTo("Alice Nguyen");
    }

    /**
     * Both stay {@code null} here; the api mapper turns them into the em dash the console has
     * always shown. Keeping the placeholder out of the application layer is what let the
     * storefront and the console phrase the same absence differently.
     */
    @Test
    void rejectReview_setsRejected_andLeavesMissingNamesToTheMapper() {
        when(reviews.findById(ReviewId.of("r1"))).thenReturn(Optional.of(pendingReview()));
        when(reviews.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(catalog.findSnapshot(ProductId.of(productHex))).thenReturn(Optional.empty());
        when(users.displayNameOf(UserId.of(userHex))).thenReturn(Optional.empty());

        AdminReviewService.ModeratedReview moderated = service.rejectReview("r1");

        assertThat(moderated.review().getStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(moderated.productTitle()).isNull();
        assertThat(moderated.authorName()).isNull();
    }

    @Test
    void approveReview_missing_throwsNotFound() {
        when(reviews.findById(ReviewId.of("nope"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveReview("nope"))
                .isInstanceOf(ReviewNotFoundException.class);
    }

    /**
     * The behaviour change that matters most for the rollup: {@code setStatus} used to re-save
     * the same status silently, so a moderator clicking approve twice would have counted the
     * same stars twice once the rollup followed moderation.
     */
    @Test
    @DisplayName("approving an already-approved review is a 409, and nothing is saved")
    void doubleApprovalIsAConflict() {
        when(reviews.findById(ReviewId.of("r1")))
                .thenReturn(Optional.of(ReviewFixture.aReview().withId("r1").approved().build()));

        assertThatThrownBy(() -> service.approveReview("r1"))
                .isInstanceOf(IllegalReviewTransitionException.class);

        verify(reviews, never()).save(any());
    }

    @Test
    @DisplayName("a review refused in error can be reinstated")
    void aRejectedReviewCanBeApproved() {
        when(reviews.findById(ReviewId.of("r1")))
                .thenReturn(Optional.of(ReviewFixture.aReview().withId("r1")
                        .of(productHex).by(userHex).rejected().build()));
        when(reviews.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(catalog.findSnapshot(any())).thenReturn(Optional.empty());
        when(users.displayNameOf(any())).thenReturn(Optional.empty());

        assertThat(service.approveReview("r1").review().getStatus())
                .isEqualTo(ReviewStatus.APPROVED);
    }
}
