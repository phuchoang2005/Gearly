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
        List<Review> reviews = reviewRepo.findAll();

        // collect unique product/user IDs
        Set<String> productIds = reviews.stream()
                .map(r -> r.getProductId().toHexString())
                .collect(Collectors.toSet());
        Set<String> userIds = reviews.stream()
                .map(r -> r.getUserId().toHexString())
                .collect(Collectors.toSet());

        // batch‐fetch products and users
        Map<String, Product> productMap = productRepo.findAllById(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, b -> b));
        Map<String, User> userMap = userRepo.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // map each review into the DTO
        return reviews.stream()
                .map(r -> {
                    String bId = r.getProductId().toHexString();
                    String uId = r.getUserId().toHexString();
                    Product product = productMap.get(bId);
                    User user = userMap.get(uId);

                    String productTitle = product != null ? product.getTitle() : "—";
                    String userName = user != null ? user.getFullName() : "—";
                    return reviewMapper.toAdminDto(r, productTitle, userName);
                })
                .collect(Collectors.toList());
    }

    public List<Review> getReviewsByStatus(ReviewStatus status) {
        return reviewRepo.findReviewByStatus(status);
    }

    public Review approveReview(String id) {
        Review review = reviewRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setStatus(ReviewStatus.APPROVED);
        return reviewRepo.save(review);
    }

    public Review rejectReview(String id) {
        Review review = reviewRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setStatus(ReviewStatus.REJECTED);
        return reviewRepo.save(review);
    }
}
