package com.dominator.gearly.service.admin;

import com.dominator.gearly.dto.AdminReviewResponseDTO;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.mapper.ReviewMapper;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.Review;
import com.dominator.gearly.model.ReviewStatus;
import com.dominator.gearly.model.User;
import com.dominator.gearly.repository.ProductRepository;
import com.dominator.gearly.repository.ReviewRepository;
import com.dominator.gearly.repository.UserRepository;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * S7: the admin review moderation endpoints return AdminReviewResponseDTO with the
 * product title and reviewer name resolved, instead of the raw Review entity.
 */
@ExtendWith(MockitoExtension.class)
class AdminReviewServiceTest {

    @Mock private ReviewRepository reviewRepo;
    @Mock private ProductRepository productRepo;
    @Mock private UserRepository userRepo;

    private AdminReviewService service;

    private final String productHex = new ObjectId().toHexString();
    private final String userHex = new ObjectId().toHexString();

    @BeforeEach
    void setUp() {
        service = new AdminReviewService(reviewRepo, productRepo, userRepo, new ReviewMapper());
    }

    private Review pendingReview() {
        Review r = new Review();
        r.setId("r1");
        r.setProductId(new ObjectId(productHex));
        r.setUserId(new ObjectId(userHex));
        r.setRating(5);
        r.setSubject("Great");
        r.setComment("Runs cool");
        r.setStatus(ReviewStatus.PENDING);
        return r;
    }

    @Test
    void approveReview_setsApproved_andResolvesTitleAndName() {
        when(reviewRepo.findById("r1")).thenReturn(Optional.of(pendingReview()));
        when(reviewRepo.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        Product product = new Product();
        product.setId(productHex);
        product.setTitle("RTX 4090");
        when(productRepo.findById(productHex)).thenReturn(Optional.of(product));
        User user = new User();
        user.setId(userHex);
        user.setFullName("Alice Nguyen");
        when(userRepo.findById(userHex)).thenReturn(Optional.of(user));

        AdminReviewResponseDTO dto = service.approveReview("r1");

        assertThat(dto.getId()).isEqualTo("r1");
        assertThat(dto.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(dto.getProductTitle()).isEqualTo("RTX 4090");
        assertThat(dto.getUserName()).isEqualTo("Alice Nguyen");
        assertThat(dto.getRating()).isEqualTo(5);
    }

    @Test
    void rejectReview_setsRejected_andFallsBackWhenProductOrUserMissing() {
        when(reviewRepo.findById("r1")).thenReturn(Optional.of(pendingReview()));
        when(reviewRepo.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepo.findById(productHex)).thenReturn(Optional.empty());
        when(userRepo.findById(userHex)).thenReturn(Optional.empty());

        AdminReviewResponseDTO dto = service.rejectReview("r1");

        assertThat(dto.getStatus()).isEqualTo(ReviewStatus.REJECTED);
        assertThat(dto.getProductTitle()).isEqualTo("—");
        assertThat(dto.getUserName()).isEqualTo("—");
    }

    @Test
    void approveReview_missing_throwsNotFound() {
        when(reviewRepo.findById("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveReview("nope"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
