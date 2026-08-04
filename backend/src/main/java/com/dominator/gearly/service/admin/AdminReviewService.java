package com.dominator.gearly.service.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.dominator.gearly.dto.AdminReviewResponseDTO;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.mapper.ReviewMapper;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.User;
import com.dominator.gearly.repository.ProductRepository;
import com.dominator.gearly.repository.UserRepository;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.dominator.gearly.model.Review;
import com.dominator.gearly.model.ReviewStatus;
import com.dominator.gearly.repository.ReviewRepository;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private final ReviewRepository reviewRepo;
    private final ProductRepository   productRepo;
    private final UserRepository   userRepo;
    private final ReviewMapper     reviewMapper;

    public List<AdminReviewResponseDTO> getAllReviews() {
        return toDtos(reviewRepo.findAll());
    }

    public List<AdminReviewResponseDTO> getReviewsByStatus(ReviewStatus status) {
        return toDtos(reviewRepo.findReviewByStatus(status));
    }

    public AdminReviewResponseDTO approveReview(String id) {
        return toDto(setStatus(id, ReviewStatus.APPROVED));
    }

    public AdminReviewResponseDTO rejectReview(String id) {
        return toDto(setStatus(id, ReviewStatus.REJECTED));
    }

    private Review setStatus(String id, ReviewStatus status) {
        Review review = reviewRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setStatus(status);
        return reviewRepo.save(review);
    }

    /** Resolves product title and author name for a batch of reviews. */
    private List<AdminReviewResponseDTO> toDtos(List<Review> reviews) {
        // collect unique product/user IDs
        Set<String> productIds = reviews.stream()
                .map(r -> r.getProductId().value())
                .collect(Collectors.toSet());
        Set<String> userIds = reviews.stream()
                .map(r -> r.getUserId().value())
                .collect(Collectors.toSet());

        // batch-fetch products and users
        Map<String, Product> productMap = productRepo.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, b -> b));
        Map<String, User> userMap = userRepo.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return reviews.stream()
                .map(r -> reviewMapper.toAdminDto(
                        r,
                        titleOf(productMap.get(r.getProductId().value())),
                        nameOf(userMap.get(r.getUserId().value()))))
                .collect(Collectors.toList());
    }

    /** Single-review variant that resolves the product/user directly. */
    private AdminReviewResponseDTO toDto(Review review) {
        Product product = productRepo.findById(review.getProductId().value()).orElse(null);
        User user = userRepo.findById(review.getUserId().value()).orElse(null);
        return reviewMapper.toAdminDto(review, titleOf(product), nameOf(user));
    }

    private String titleOf(Product product) {
        return product != null ? product.getTitle() : "—";
    }

    private String nameOf(User user) {
        return user != null ? user.getFullName() : "—";
    }
}
