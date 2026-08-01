package com.dominator.gearly.service.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.dominator.gearly.dto.AdminReviewResponseDTO;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.mapper.ReviewMapper;
import com.dominator.gearly.model.Book;
import com.dominator.gearly.model.User;
import com.dominator.gearly.repository.BookRepository;
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
    private final BookRepository   bookRepo;
    private final UserRepository   userRepo;
    private final ReviewMapper     reviewMapper;

    public List<AdminReviewResponseDTO> getAllReviews() {
        List<Review> reviews = reviewRepo.findAll();

        // collect unique book/user IDs
        Set<String> bookIds = reviews.stream()
                .map(r -> r.getBookId().toHexString())
                .collect(Collectors.toSet());
        Set<String> userIds = reviews.stream()
                .map(r -> r.getUserId().toHexString())
                .collect(Collectors.toSet());

        // batch‐fetch books and users
        Map<String, Book> bookMap = bookRepo.findAllById(bookIds)
                .stream()
                .collect(Collectors.toMap(Book::getId, b -> b));
        Map<String, User> userMap = userRepo.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // map each review into the DTO
        return reviews.stream()
                .map(r -> {
                    String bId = r.getBookId().toHexString();
                    String uId = r.getUserId().toHexString();
                    Book book = bookMap.get(bId);
                    User user = userMap.get(uId);

                    String bookTitle = book != null ? book.getTitle() : "—";
                    String userName = user != null ? user.getFullName() : "—";
                    return reviewMapper.toAdminDto(r, bookTitle, userName);
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
