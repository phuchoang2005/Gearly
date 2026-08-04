package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.*;
import com.dominator.gearly.mapper.ReviewMapper;
import com.dominator.gearly.model.*;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.shared.domain.UserId;
import com.dominator.gearly.repository.ProductRepository;
import com.dominator.gearly.repository.OrderRepository;
import com.dominator.gearly.repository.ReviewRepository;
import com.dominator.gearly.repository.UserRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dominator.gearly.exception.ApiException;
import com.dominator.gearly.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ReviewMapper reviewMapper;

    public Page<ReviewResponseDTO> getApprovedReviews(ProductReviewsDTO dto) {
        Pageable pageable = PageRequest.of(dto.getPageIndex(), dto.getPageSize(),
                Sort.by(Sort.Direction.DESC, dto.getSortBy()));
        ObjectId productObjectId = new ObjectId(dto.getProductId());

        Page<Review> reviewPage = dto.getRating() == 0
                ? reviewRepository.findByProductIdAndStatus(productObjectId, ReviewStatus.APPROVED, pageable)
                : reviewRepository.findByProductIdAndStatusAndRating(productObjectId, ReviewStatus.APPROVED, dto.getRating(),
                        pageable);

        return reviewPage.map(review -> {
            String userName = userRepository.findById(review.getUserId().toString())
                    .map(User::getFullName)
                    .orElse("Unknown User");

            return reviewMapper.toResponseDto(review, userName);
        });
    }

    public List<ReviewResponseDTO> getSixBestReviews() {
        List<Review> topReviews = reviewRepository.findTopByRatingAndStatusGroupedByUser(5, ReviewStatus.APPROVED,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "addedAt")));
        Map<String, Review> uniqueUsers = new LinkedHashMap<>();

        for (Review r : topReviews) {
            String userId = r.getUserId().toString();
            if (!uniqueUsers.containsKey(userId)) {
                uniqueUsers.put(userId, r);
                if (uniqueUsers.size() == 6)
                    break;
            }
        }

        return uniqueUsers.values().stream().map(review -> {
            String userName = userRepository.findById(review.getUserId().toString())
                    .map(User::getFullName)
                    .orElse("Unknown User");

            return reviewMapper.toResponseDto(review, userName);
        }).collect(Collectors.toList());
    }

    public List<ReviewRatingDTO> getRatingDistribution(String productId) {
        ObjectId objectId = new ObjectId(productId);
        List<Map<String, Object>> results = reviewRepository.getRatingDistribution(objectId);

        Map<Integer, Long> counts = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++)
            counts.put(i, 0L);

        for (Map<String, Object> row : results) {
            int stars = ((Number) row.get("_id")).intValue();
            long count = ((Number) row.get("count")).longValue();
            if (stars >= 1 && stars <= 5) {
                counts.put(stars, count);
            }
        }

        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        List<ReviewRatingDTO> distribution = new ArrayList<>();
        for (int i = 5; i >= 1; i--) {
            long count = counts.get(i);
            double percentage = total > 0 ? (count * 100.0 / total) : 0.0;
            distribution.add(new ReviewRatingDTO(i, count, Math.round(percentage)));
        }

        return distribution;
    }

    @Transactional
    public void createReview(AuthenticatedUser authUser, CreateReviewsRequestDTO dto) {
        User user = authUser.getUser();
        Order order = requireOwnedOrder(user, dto.getOrderId());
        Map<String, Product> productMap = loadProducts(dto.getReviews());

        List<Product> productsToSave = new ArrayList<>();
        List<Review> reviewsToSave = new ArrayList<>();

        for (CreateReviewRequestDTO rdto : dto.getReviews()) {
            Product product = productMap.get(rdto.getProductId());
            if (product == null) {
                throw new ResourceNotFoundException(
                        "Product not found, you cannot create review for this product.");
            }
            applyRating(product, rdto.getRating());
            productsToSave.add(product);
            reviewsToSave.add(reviewMapper.toEntity(rdto, dto.getOrderId(), user.getId()));
        }

        productRepository.saveAll(productsToSave);
        reviewRepository.saveAll(reviewsToSave);
        order.markReviewed();
        orderRepository.save(order);
    }

    private Order requireOwnedOrder(User user, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found, you cannot create review on this."));
        if (!order.isOwnedBy(UserId.of(user.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "You are not allowed to review the items in this order");
        }
        return order;
    }

    private Map<String, Product> loadProducts(List<CreateReviewRequestDTO> reviews) {
        List<String> productIds = reviews.stream()
                .map(CreateReviewRequestDTO::getProductId)
                .collect(Collectors.toList());
        return productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
    }

    private void applyRating(Product product, int rating) {
        int newCount = product.getRatingCount() + 1;
        int newTotal = product.getTotalRating() + rating;
        product.setRatingCount(newCount);
        product.setTotalRating(newTotal);
        double average = Math.round((double) newTotal / newCount * 100) / 100.0;
        product.setAverageRating(average);
    }
}
