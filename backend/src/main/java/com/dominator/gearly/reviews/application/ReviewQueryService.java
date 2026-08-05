package com.dominator.gearly.reviews.application;

import com.dominator.gearly.identity.domain.UserDirectory;
import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewPage;
import com.dominator.gearly.reviews.domain.ReviewRepository;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The public reads: a product's review list, the storefront's testimonial strip, and the star
 * histogram.
 *
 * <p>Author names come from {@code UserDirectory}, identity's published port, and are resolved
 * in one batched call per page. The old code called {@code userRepository.findById} once per
 * review — inside the {@code map} that built each DTO — so a page of ten reviews was eleven
 * queries, and each one loaded a whole {@code User} to read one field.
 */
@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    /**
     * How many top-rated reviews to read before de-duplicating by author. Twenty, as it always
     * was: the strip shows six, and reading a few extra is what makes room for several reviews
     * by the same person.
     */
    private static final int TESTIMONIAL_CANDIDATES = 20;

    /** How many distinct authors the testimonial strip shows. */
    private static final int TESTIMONIALS = 6;

    private final ReviewRepository reviews;
    private final UserDirectory users;

    /**
     * One page of a product's published reviews.
     *
     * @param rating {@code 0} means "any rating", as the storefront has always sent it
     */
    public PagedReviews approvedFor(ProductId productId, int rating, int page, int size, String sortBy) {
        ReviewPage found = reviews.findApproved(
                productId, rating == 0 ? null : Rating.of(rating), page, size, sortBy);
        return new PagedReviews(withAuthors(found.content()), found.page(), found.size(),
                found.totalElements());
    }

    /**
     * The testimonial strip: five-star reviews, at most one per author, at most six.
     *
     * <p>The de-duplication keeps the <em>first</em> review each author appears with, which —
     * given the repository sorts newest first — is their most recent. Unchanged behaviour,
     * stated because it is not obvious from the loop.
     */
    public List<AuthoredReview> testimonials() {
        Map<UserId, Review> oneEach = new LinkedHashMap<>();
        for (Review review : reviews.findTopRated(Rating.of(Rating.MAX), TESTIMONIAL_CANDIDATES)) {
            oneEach.putIfAbsent(review.getUserId(), review);
            if (oneEach.size() == TESTIMONIALS) {
                break;
            }
        }
        return withAuthors(List.copyOf(oneEach.values()));
    }

    /**
     * How many published reviews a product has at each star count, five down to one.
     *
     * <p>Always five entries, including the empty ones, and a product with no reviews gets
     * zeroes rather than a divide-by-zero.
     */
    public List<RatingShare> distributionFor(ProductId productId) {
        Map<Rating, Long> tally = reviews.ratingTally(productId);
        long total = tally.values().stream().mapToLong(Long::longValue).sum();

        List<RatingShare> distribution = new ArrayList<>();
        for (int stars = Rating.MAX; stars >= Rating.MIN; stars--) {
            long count = tally.getOrDefault(Rating.of(stars), 0L);
            double percentage = total > 0 ? Math.round(count * 100.0 / total) : 0.0;
            distribution.add(new RatingShare(stars, count, percentage));
        }
        return distribution;
    }

    /** One lookup for the whole page; an author whose account is gone falls back at the edge. */
    private List<AuthoredReview> withAuthors(List<Review> found) {
        Map<UserId, String> names = users.displayNamesOf(
                found.stream().map(Review::getUserId).distinct().toList());

        return found.stream()
                .map(review -> new AuthoredReview(review, names.get(review.getUserId())))
                .toList();
    }

    /** One page of reviews with their authors resolved. */
    public record PagedReviews(List<AuthoredReview> content, int page, int size, long totalElements) {
    }

    /** One bar of the star histogram: how many, and what share of the total. */
    public record RatingShare(int stars, long count, double percentage) {
    }
}
