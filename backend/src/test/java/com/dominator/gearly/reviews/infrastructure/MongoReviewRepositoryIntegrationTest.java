package com.dominator.gearly.reviews.infrastructure;

import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewFixture;
import com.dominator.gearly.reviews.domain.ReviewPage;
import com.dominator.gearly.reviews.domain.ReviewRepository;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Rating;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The review repository adapter, against a real MongoDB.
 *
 * <p><b>Why this exists.</b> S12 normalized {@code reviews.productId} from a BSON
 * {@code ObjectId} to a plain string, which changed the parameter type of every query in
 * {@code SpringDataReviewRepository} — including the {@code @Aggregation} pipeline behind the
 * storefront's star histogram, where the id is bound into {@code $match} as a placeholder.
 * <b>Getting that binding wrong returns an empty result rather than failing</b>, so a unit test
 * with a mocked repository cannot see it: the histogram would simply render as all zeroes and
 * the paged review list as empty. Only a real Mongo can tell the difference.
 *
 * <p>{@code DomainTypeBsonRoundTripTest} covers the other half — what the bytes on disk are.
 * This covers whether the queries reach them.
 *
 * <p>Docker-gated, so {@code mvn test} still passes offline.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("the review queries reach documents whose ids are stored as strings")
class MongoReviewRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @Autowired private ReviewRepository reviews;
    @Autowired private MongoTemplate mongoTemplate;

    /** Hex-shaped, because the real ids are: a string query must match them as strings anyway. */
    private static final String PRODUCT_HEX = "682023424a1ae581e0445357";
    private static final String OTHER_PRODUCT_HEX = "682023424a1ae581e0445358";
    private static final ProductId PRODUCT = ProductId.of(PRODUCT_HEX);

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Review.class);
    }

    private void saveApproved(String productHex, int stars) {
        reviews.save(ReviewFixture.aReview().of(productHex).rated(stars).approved().build());
    }

    @Test
    @DisplayName("the star histogram counts approved reviews of the right product")
    void ratingTallyMatchesOnTheStoredStringId() {
        saveApproved(PRODUCT_HEX, 5);
        saveApproved(PRODUCT_HEX, 5);
        saveApproved(PRODUCT_HEX, 3);
        saveApproved(OTHER_PRODUCT_HEX, 1);
        reviews.save(ReviewFixture.aReview().of(PRODUCT_HEX).rated(4).build()); // still PENDING

        Map<Rating, Long> tally = reviews.ratingTally(PRODUCT);

        assertThat(tally).containsOnly(
                Map.entry(Rating.of(5), 2L),
                Map.entry(Rating.of(3), 1L));
    }

    @Test
    @DisplayName("the public review list is paged over the same documents")
    void findApprovedMatchesOnTheStoredStringId() {
        saveApproved(PRODUCT_HEX, 5);
        saveApproved(PRODUCT_HEX, 3);
        saveApproved(OTHER_PRODUCT_HEX, 5);
        reviews.save(ReviewFixture.aReview().of(PRODUCT_HEX).rated(2).build()); // still PENDING

        ReviewPage all = reviews.findApproved(PRODUCT, null, 0, 10, "addedAt");
        assertThat(all.totalElements()).isEqualTo(2);
        assertThat(all.content())
                .extracting(review -> review.getProductId().value())
                .containsOnly(PRODUCT_HEX);

        ReviewPage fiveStar = reviews.findApproved(PRODUCT, Rating.of(5), 0, 10, "addedAt");
        assertThat(fiveStar.totalElements()).isEqualTo(1);
        assertThat(fiveStar.content().getFirst().getRating()).isEqualTo(Rating.of(5));
    }

    @Test
    @DisplayName("a review loads back with the ids it was saved with")
    void anIdRoundTripsThroughTheAdapter() {
        Review saved = reviews.save(ReviewFixture.aReview()
                .of(PRODUCT_HEX).from("682f7504df103bcceb44d284").by("68201e5b4ff90d7e8d39395c")
                .rated(4).build());

        assertThat(reviews.findById(saved.reviewId())).hasValueSatisfying(loaded -> {
            assertThat(loaded.getProductId().value()).isEqualTo(PRODUCT_HEX);
            assertThat(loaded.getOrderId().value()).isEqualTo("682f7504df103bcceb44d284");
            assertThat(loaded.getUserId().value()).isEqualTo("68201e5b4ff90d7e8d39395c");
        });
    }
}
