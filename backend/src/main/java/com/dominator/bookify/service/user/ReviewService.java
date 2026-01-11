package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.*;
import com.dominator.bookify.model.*;
import com.dominator.bookify.repository.BookRepository;
import com.dominator.bookify.repository.OrderRepository;
import com.dominator.bookify.repository.ReviewRepository;
import com.dominator.bookify.repository.UserRepository;
import com.dominator.bookify.security.AuthenticatedUser;
import lombok.AllArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
    private final BookRepository bookRepository;

    public Page<ReviewResponseDTO> getApprovedReviews(BookReviewsDTO dto) {
        Pageable pageable = PageRequest.of(dto.getPageIndex(), dto.getPageSize(),
                Sort.by(Sort.Direction.DESC, dto.getSortBy()));
        ObjectId bookObjectId = new ObjectId(dto.getBookId());

        Page<Review> reviewPage = dto.getRating() == 0
                ? reviewRepository.findByBookIdAndStatus(bookObjectId, ReviewStatus.APPROVED, pageable)
                : reviewRepository.findByBookIdAndStatusAndRating(bookObjectId, ReviewStatus.APPROVED, dto.getRating(),
                        pageable);

        return reviewPage.map(review -> {
            String userName = userRepository.findById(review.getUserId().toString())
                    .map(User::getFullName)
                    .orElse("Unknown User");

            return new ReviewResponseDTO(
                    review.getId(),
                    review.getRating(),
                    review.getSubject(),
                    review.getComment(),
                    review.getAddedAt(),
                    userName);
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

            return new ReviewResponseDTO(
                    review.getId(),
                    review.getRating(),
                    review.getSubject(),
                    review.getComment(),
                    review.getAddedAt(),
                    userName);
        }).collect(Collectors.toList());
    }

    public List<ReviewRatingDTO> getRatingDistribution(String bookId) {
        ObjectId objectId = new ObjectId(bookId);
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
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Order not found, you cannot create review on this."));
        if (!order.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You are not allowed to review the items in this order");
        }

        List<String> bookIds = dto.getReviews().stream()
                .map(CreateReviewRequestDTO::getBookId)
                .collect(Collectors.toList());
        Map<String, Book> bookMap = bookRepository.findAllById(bookIds).stream()
                .collect(Collectors.toMap(Book::getId, Function.identity()));

        List<Book> booksToSave = new ArrayList<>();
        List<Review> reviewsToSave = new ArrayList<>();

        for (CreateReviewRequestDTO rdto : dto.getReviews()) {
            Book book = bookMap.get(rdto.getBookId());
            if (book == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Book not found, you cannot create review for this book.");
            }
            int newCount = book.getRatingCount() + 1;
            int newTotal = book.getTotalRating() + rdto.getRating();
            book.setRatingCount(newCount);
            book.setTotalRating(newTotal);
            double average = Math.round((double) newTotal / newCount * 100) / 100.0;
            book.setAverageRating(average);
            booksToSave.add(book);

            Review review = new Review();
            review.setBookId(new ObjectId(rdto.getBookId()));
            review.setOrderId(new ObjectId(dto.getOrderId()));
            review.setUserId(new ObjectId(user.getId()));
            review.setRating(rdto.getRating());
            review.setSubject(rdto.getSubject());
            review.setComment(rdto.getComment());
            reviewsToSave.add(review);
        }

        bookRepository.saveAll(booksToSave);
        reviewRepository.saveAll(reviewsToSave);
        order.setReviewed(true);
        orderRepository.save(order);
    }
}
