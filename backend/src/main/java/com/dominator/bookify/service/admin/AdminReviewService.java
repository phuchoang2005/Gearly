package com.dominator.bookify.service.admin;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.dominator.bookify.dto.AdminReviewResponseDTO;
import com.dominator.bookify.model.Book;
import com.dominator.bookify.model.User;
import com.dominator.bookify.repository.BookRepository;
import com.dominator.bookify.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dominator.bookify.model.Review;
import com.dominator.bookify.model.ReviewStatus;
import com.dominator.bookify.repository.ReviewRepository;

@Service
public class AdminReviewService {

    private final ReviewRepository reviewRepo;
    private final BookRepository   bookRepo;
    private final UserRepository   userRepo;

    @Autowired  // <— this tells Spring “use this constructor”
    public AdminReviewService(
            ReviewRepository reviewRepo,
            BookRepository   bookRepo,
            UserRepository   userRepo
    ) {
        this.reviewRepo = reviewRepo;
        this.bookRepo   = bookRepo;
        this.userRepo   = userRepo;
    }

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

                    return new AdminReviewResponseDTO(
                            r.getId(),
                            book != null ? book.getTitle() : "—",
                            user != null ? user.getFullName() : "—",
                            r.getRating(),
                            r.getSubject(),
                            r.getComment(),
                            r.getStatus(),
                            r.getAddedAt(),
                            r.getModifiedAt()
                    );
                })
                .collect(Collectors.toList());
    }

    public List<Review> getReviewsByStatus(ReviewStatus status) {
        return reviewRepo.findReviewByStatus(status);
    }

    public Review approveReview(String id) {
        Review review = reviewRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setStatus(ReviewStatus.APPROVED);
        return reviewRepo.save(review);
    }

    public Review rejectReview(String id) {
        Review review = reviewRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Review not found"));
        review.setStatus(ReviewStatus.REJECTED);
        return reviewRepo.save(review);
    }
}
