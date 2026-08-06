package com.dominator.gearly.shared.infrastructure;

import com.dominator.gearly.dto.BestSellerDTO;
import com.dominator.gearly.cart.domain.Cart;
import com.dominator.gearly.cart.domain.CartFixture;
import com.dominator.gearly.catalog.domain.Category;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.catalog.domain.ProductFixture;
import com.dominator.gearly.reviews.domain.Review;
import com.dominator.gearly.reviews.domain.ReviewFixture;
import com.dominator.gearly.identity.domain.UserFixture;
import com.dominator.gearly.shared.domain.CategoryId;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Role;
import com.dominator.gearly.shared.domain.UserId;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <b>The proof that S9 needed no data migration.</b>
 *
 * <p>Introducing the value objects rests entirely on one claim: that every converter in
 * {@link DomainTypeConverters} writes back the same BSON type the documents already hold.
 * Nothing in the unit tests can check that — they exercise Java objects, and a round trip
 * through the mapper would pass just as happily if {@code Money} were stored as a nested
 * {@code {amount, currency}} document, because it would read back the same way.
 *
 * <p>So these tests read the <em>raw</em> {@link Document} straight out of Mongo, bypassing
 * the entity mapping entirely, and assert the concrete stored class of each field. That is
 * the difference between "it round-trips" and "the bytes on disk are unchanged".
 *
 * <p>Docker-gated ({@code disabledWithoutDocker}) so {@code mvn test} still passes offline;
 * run with Colima up to actually exercise them.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("stored BSON types are unchanged by the value objects")
class DomainTypeBsonRoundTripTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @Autowired
    private MongoTemplate mongoTemplate;

    private static final String CATEGORY_HEX = "694ae1e055e2e3dc4b3500e6";
    private static final String PRODUCT_HEX = "682023424a1ae581e0445357";
    private static final String ORDER_HEX = "682f7504df103bcceb44d284";
    private static final String USER_HEX = "68201e5b4ff90d7e8d39395c";

    @BeforeEach
    void setUp() {
        mongoTemplate.getDb().drop();
    }

    /** The raw document, read with no entity mapping in the way. */
    private Document rawDocument(String collection) {
        Document raw = mongoTemplate.getCollection(collection).find().first();
        assertThat(raw).as("a document in '%s'", collection).isNotNull();
        return raw;
    }

    @Nested
    @DisplayName("Money is stored as a double")
    class MoneyStorage {

        @Test
        void onAProductAndItsOriginalPrice() {
            Product product = ProductFixture.aProduct()
                    .titled("RTX 4090").pricedAt(109.99).originallyPricedAt(120.5).build();
            mongoTemplate.save(product);

            Document raw = rawDocument("products");

            assertThat(raw.get("price")).isInstanceOf(Double.class).isEqualTo(109.99);
            assertThat(raw.get("originalPrice")).isInstanceOf(Double.class).isEqualTo(120.5);
        }

        @Test
        void onAnOrderTotalAndItsLines() {
            // 1599.00 x 2 = 3198.00, + 8% tax 255.84, free shipping -> 3453.84
            Order order = OrderFixture.anOrder()
                    .withLines(OrderFixture.line("p1", "GPU", 1599.0, 2))
                    .build();
            mongoTemplate.save(order);

            Document raw = rawDocument("orders");

            assertThat(raw.get("totalAmount")).isInstanceOf(Double.class).isEqualTo(3453.84);
            Document line = raw.getList("items", Document.class).getFirst();
            assertThat(line.get("price")).isInstanceOf(Double.class).isEqualTo(1599.0);

            // S10 adopted UserId, ProductId and Quantity on the order aggregate. Same claim as
            // S9's for Money: the stored BSON must not have noticed. Read raw, so a nested
            // {value: …} document could not pass by reading back the same way it was written.
            assertThat(raw.get("userId")).isInstanceOf(String.class).isEqualTo("u1");
            assertThat(line.get("productId")).isInstanceOf(String.class).isEqualTo("p1");
            assertThat(line.get("quantity")).isInstanceOf(Integer.class).isEqualTo(2);
            assertThat(raw.get("orderStatus")).isInstanceOf(String.class).isEqualTo("PENDING");
        }

        @Test
        void onAnEmbeddedPaymentTransaction() {
            // 10.00 x 2 = 20.00, + 8% tax 1.60, + 15.00 shipping -> the opening charge is 36.60
            Order order = OrderFixture.anOrder()
                    .withLines(OrderFixture.line("p1", "GPU", 10.0, 2))
                    .paidWith("momo")
                    .build();
            mongoTemplate.save(order);

            Document payment = rawDocument("orders").get("payment", Document.class);
            Document stored = payment.getList("transactions", Document.class).getFirst();

            assertThat(stored.get("amount")).isInstanceOf(Double.class).isEqualTo(36.6);
        }

        @Test
        void onACartLine() {
            Cart cart = CartFixture.aCart().ownedBy("u1")
                    .holding("p1", 24.99, 5, 1)
                    .build();
            mongoTemplate.save(cart);

            Document line = rawDocument("carts").getList("items", Document.class).getFirst();

            assertThat(line.get("price")).isInstanceOf(Double.class).isEqualTo(24.99);
            // The typed id and the two quantities are as bare as they ever were.
            assertThat(line.get("productId")).isInstanceOf(String.class).isEqualTo("p1");
            assertThat(line.get("quantity")).isInstanceOf(Integer.class).isEqualTo(1);
            assertThat(line.get("stock")).isInstanceOf(Integer.class).isEqualTo(5);
            assertThat(rawDocument("carts").get("userId")).isInstanceOf(String.class).isEqualTo("u1");
        }

        /**
         * Integral prices are in the seed data as BSON {@code int32}
         * ({@code "originalPrice": 195}). Reading has to accept that; writing normalizes to
         * a double, which is exactly what the previous {@code double} field already did on
         * every save.
         */
        @Test
        void readsAnIntegerPriceWrittenByOlderData() {
            mongoTemplate.getCollection("products")
                    .insertOne(new Document("_id", new ObjectId(PRODUCT_HEX))
                            .append("title", "Legacy")
                            .append("price", 195)                 // int32
                            .append("originalPrice", 195L)        // int64
                            .append("version", 0L));

            Product loaded = mongoTemplate.findById(PRODUCT_HEX, Product.class);

            assertThat(loaded).isNotNull();
            assertThat(loaded.getPrice()).isEqualTo(Money.of("195.00"));
            assertThat(loaded.getOriginalPrice()).isEqualTo(Money.of("195.00"));
        }
    }

    @Nested
    @DisplayName("a typed id is a string everywhere except a category id")
    class IdStorage {

        /** Category ids are the one id stored as an ObjectId rather than a string. */
        @Test
        void categoryIdIsStoredAsAnObjectId() {
            Product product = ProductFixture.aProduct()
                    .titled("RTX 4090").inCategories(CategoryId.of(CATEGORY_HEX)).build();
            mongoTemplate.save(product);

            Object stored = rawDocument("products").getList("categoryIds", Object.class).getFirst();

            assertThat(stored).isInstanceOf(ObjectId.class);
            assertThat(stored).isEqualTo(new ObjectId(CATEGORY_HEX));
        }

        /**
         * <b>Rewritten in S12, deliberately.</b> Until this sprint a review stored its three
         * ids as {@code ObjectId}s while the same Java types were strings on an order line —
         * the asymmetry S9 absorbed behind {@code ObjectIdBackedIdConverters} and explicitly
         * left for S12 to decide about. S12 normalized it, so the assertion that pinned the
         * {@code ObjectId} form now pins the string form, and the order line beside it —
         * unchanged since S10 — is what makes "the same everywhere" checkable in one test.
         *
         * <p>{@code data/seed/migrate.js} step 11 is what moves existing documents.
         */
        @Test
        void reviewIdsAreStringsLikeEveryOtherTypedIdOutsideCategories() {
            mongoTemplate.save(ReviewFixture.aReview()
                    .of(PRODUCT_HEX).from(ORDER_HEX).by(USER_HEX).rated(5).build());

            Document rawReview = rawDocument("reviews");
            assertThat(rawReview.get("productId")).isInstanceOf(String.class)
                    .isEqualTo(PRODUCT_HEX);
            assertThat(rawReview.get("orderId")).isInstanceOf(String.class).isEqualTo(ORDER_HEX);
            assertThat(rawReview.get("userId")).isInstanceOf(String.class).isEqualTo(USER_HEX);
            // S12 adopted the Rating value object on this field; it still writes as an int32.
            assertThat(rawReview.get("rating")).isInstanceOf(Integer.class).isEqualTo(5);

            Order order = OrderFixture.anOrder()
                    .withLines(OrderFixture.line(PRODUCT_HEX, "GPU", 0.0, 1))
                    .build();
            mongoTemplate.save(order);

            Document line = rawDocument("orders").getList("items", Document.class).getFirst();
            assertThat(line.get("productId")).isInstanceOf(String.class).isEqualTo(PRODUCT_HEX);
        }

        /**
         * The queries that read the reviews collection have to reach the normalized documents.
         *
         * <p>This is the S12 counterpart of {@code aReviewRemainsQueryableByRawObjectId}: the
         * old test proved the rating-distribution aggregation could still match on the raw
         * {@code ObjectId}, and it is now false by construction. What has to be true instead
         * is that a match on the raw <em>string</em> finds the document — and, because the
         * failure mode of getting this wrong is silence rather than an exception, that a match
         * on the {@code ObjectId} finds nothing.
         */
        @Test
        void aReviewIsQueryableByTheRawStringAndNotByAnObjectId() {
            mongoTemplate.save(ReviewFixture.aReview()
                    .of(PRODUCT_HEX).by(USER_HEX).rated(4).build());

            assertThat(mongoTemplate.getCollection("reviews")
                    .countDocuments(new Document("productId", PRODUCT_HEX)))
                    .isEqualTo(1);
            assertThat(mongoTemplate.getCollection("reviews")
                    .countDocuments(new Document("productId", new ObjectId(PRODUCT_HEX))))
                    .isZero();
        }
    }

    @Nested
    @DisplayName("enums keep their stored token")
    class EnumStorage {

        @Test
        void productConditionStoresTheSpacedWireValueNotTheConstantName() {
            Product product = ProductFixture.aProduct()
                    .titled("RTX 4090").inCondition(ProductCondition.LIKE_NEW).build();
            mongoTemplate.save(product);

            assertThat(rawDocument("products").get("condition"))
                    .isInstanceOf(String.class)
                    .isEqualTo("LIKE NEW");
        }

        @Test
        void roleStoresItsConstantName() {
            mongoTemplate.save(UserFixture.aUser().withEmail("ada@example.com").asAdmin().build());

            assertThat(rawDocument("users").get("role")).isEqualTo("ADMIN");
        }

        /**
         * The identity aggregate adopted {@code EmailAddress}, {@code PhoneNumber} and
         * {@code List<ProductId>} in S12, and none of them may change what is on disk. Read
         * off the raw {@code org.bson.Document}, not off a reloaded entity: a save-then-load
         * assertion would pass just as happily with the address stored as a nested
         * {@code {value: …}}, because it would read back the same way.
         */
        @Test
        void identityValueObjectsStayFlatStringsOnDisk() {
            mongoTemplate.save(UserFixture.aUser()
                    .withEmail("ada@example.com")
                    .withPhone("0123456789")
                    .favouring(PRODUCT_HEX)
                    .build());

            Document stored = rawDocument("users");

            assertThat(stored.get("email")).isInstanceOf(String.class).isEqualTo("ada@example.com");
            assertThat(stored.get("phone")).isInstanceOf(String.class).isEqualTo("0123456789");
            assertThat(stored.getList("favorites", Object.class))
                    .allSatisfy(id -> assertThat(id).isInstanceOf(String.class))
                    .containsExactly(PRODUCT_HEX);
        }

        /**
         * A pre-S9 document is only readable if the stored token still parses. This is the
         * case that would have broken had the enum relied on {@code valueOf}.
         */
        @Test
        void readsAConditionWrittenBeforeTheEnumExisted() {
            mongoTemplate.getCollection("products")
                    .insertOne(new Document("_id", new ObjectId(PRODUCT_HEX))
                            .append("title", "Legacy")
                            .append("condition", "LIKE NEW")
                            .append("version", 0L));

            Product loaded = mongoTemplate.findById(PRODUCT_HEX, Product.class);

            assertThat(loaded).isNotNull();
            assertThat(loaded.getCondition()).isEqualTo(ProductCondition.LIKE_NEW);
        }
    }

    /**
     * The other half of S9's storage change: timestamps that were stored as strings (or as
     * a {@code LocalDateTime}) become real BSON dates. Unlike the value objects, this one
     * <em>does</em> move the stored bytes — {@code data/seed/migrate.js} step 7 is what
     * converts existing documents, and the seed dumps ship already converted.
     */
    @Nested
    @DisplayName("normalized timestamps are stored as BSON dates")
    class TimestampStorage {

        @Test
        void categoryTimestampsAreDatesNotStrings() {
            Category category = Category.create("CPU", null, null);
            mongoTemplate.save(category);

            Document raw = rawDocument("categories");

            assertThat(raw.get("addedAt")).isInstanceOf(java.util.Date.class);
            assertThat(raw.get("modifiedAt")).isInstanceOf(java.util.Date.class);
        }

        @Test
        void reviewTimestampsAreDatesNotStrings() {
            mongoTemplate.save(ReviewFixture.aReview()
                    .of(PRODUCT_HEX).by(USER_HEX).rated(5)
                    .persistedAs(null, Instant.parse("2025-05-23T01:57:38.580Z"))
                    .build());

            assertThat(rawDocument("reviews").get("addedAt")).isInstanceOf(java.util.Date.class);
        }

        /**
         * Carts were already stored as BSON dates — the {@code Date} to {@code Instant}
         * change is Java-side only, which is why no migration step covers them.
         */
        @Test
        void cartTimestampsWereAlreadyDatesAndStayThatWay() {
            Cart cart = CartFixture.aCart().ownedBy("u1").build();
            mongoTemplate.save(cart);

            Document raw = rawDocument("carts");

            // The values themselves are not asserted: @CreatedDate/@LastModifiedDate
            // auditing overwrites them on save. The stored BSON type is the point.
            assertThat(raw.get("createdAt")).isInstanceOf(java.util.Date.class);
            assertThat(raw.get("updatedAt")).isInstanceOf(java.util.Date.class);
        }

        /**
         * The migration's job in one assertion: a document written before S9, carrying the
         * string form, still loads.
         */
        /**
         * Why the migration is not optional, stated precisely.
         *
         * <p>A zone-qualified ISO string still coerces into an {@code Instant} on read —
         * Spring Data's conversion service handles it — so the 10 categories and 51 reviews
         * holding that shape would have survived the type change unnoticed. Their problem
         * was never readability but ordering: stored as strings they sort
         * lexicographically, which only coincides with chronological order while every
         * value shares one format.
         */
        @Test
        void aZoneQualifiedIsoStringStillCoercesOnRead() {
            mongoTemplate.getCollection("categories")
                    .insertOne(new Document("_id", new ObjectId(CATEGORY_HEX))
                            .append("name", "Legacy")
                            .append("addedAt", "2025-12-24T00:00:00.000Z"));

            Category loaded = mongoTemplate.findById(CATEGORY_HEX, Category.class);

            assertThat(loaded).isNotNull();
            assertThat(loaded.getAddedAt()).isEqualTo(Instant.parse("2025-12-24T00:00:00Z"));
        }

        /**
         * <b>The document shape that makes migrate.js step 7 mandatory.</b>
         *
         * <p>40 of the 91 seeded reviews store their timestamps as an en-US
         * {@code toLocaleString()} value — {@code "6/9/25, 3:42 AM"}. That does not coerce,
         * so after the {@code String -> Instant} change those reviews become unreadable and
         * every read path touching them fails. This was found by running the migration
         * against the real dumps; the sprint plan anticipated only the two ISO shapes.
         */
        @Test
        void anEnUsLocaleTimestampDoesNotCoerce_whichIsWhatTheMigrationFixes() {
            mongoTemplate.getCollection("reviews")
                    .insertOne(new Document("_id", new ObjectId(ORDER_HEX))
                            .append("rating", 5)
                            .append("addedAt", "6/9/25, 3:42 AM"));

            assertThatThrownBy(() -> mongoTemplate.findById(ORDER_HEX, Review.class))
                    .hasRootCauseInstanceOf(java.time.format.DateTimeParseException.class);
        }

        @Test
        void migratedCategoryAndReviewDocumentsLoadCleanly() {
            mongoTemplate.getCollection("categories")
                    .insertOne(new Document("_id", new ObjectId(CATEGORY_HEX))
                            .append("name", "CPU")
                            .append("addedAt", java.util.Date.from(Instant.parse("2025-12-24T00:00:00Z"))));

            Category loaded = mongoTemplate.findById(CATEGORY_HEX, Category.class);

            assertThat(loaded).isNotNull();
            assertThat(loaded.getAddedAt()).isEqualTo(Instant.parse("2025-12-24T00:00:00Z"));
        }
    }

    /**
     * The end-to-end shape check: save a fully populated product, then compare the raw
     * document field-for-field against the BSON types the pre-S9 seed dump holds.
     */
    @Test
    @DisplayName("a fully populated product document matches the pre-S9 field types exactly")
    void productDocumentShapeIsUnchanged() {
        Product product = ProductFixture.aProduct()
                .titled("Intel Core i3-12100F")
                .by("Intel")
                .described("entry-level processor")
                .pricedAt(109.99)
                .originallyPricedAt(120.5)
                .inCondition(ProductCondition.NEW)
                .withStock(64)
                .inCategories(CategoryId.of(CATEGORY_HEX))
                .rated(5, 4, 5, 4, 5, 4, 5, 4, 5, 4, 5, 4)
                .build();
        mongoTemplate.save(product);

        Document raw = rawDocument("products");

        assertThat(raw.get("title")).isInstanceOf(String.class);
        assertThat(raw.get("price")).isInstanceOf(Double.class);
        assertThat(raw.get("originalPrice")).isInstanceOf(Double.class);
        assertThat(raw.get("condition")).isInstanceOf(String.class);
        assertThat(raw.get("stock")).isInstanceOf(Integer.class);
        assertThat(raw.getList("categoryIds", Object.class).getFirst()).isInstanceOf(ObjectId.class);
        assertThat(raw.get("averageRating")).isInstanceOf(Double.class);
        assertThat(raw.get("ratingCount")).isInstanceOf(Integer.class);
        assertThat(raw.get("totalRating")).isInstanceOf(Integer.class);
        assertThat(raw.get("version")).isInstanceOf(Long.class);

        // categoryNames is @Transient — a read-model field that must never be persisted
        assertThat(raw).doesNotContainKey("categoryNames");
    }

    /**
     * Aggregation projections do not go through a mapper, so nothing in the mapper tests
     * covers them. {@code BestSellerDTO} is filled straight from a {@code $project} stage;
     * its {@code Money} field has to be populated by the same converter pair the entities
     * use, or the admin dashboard's top-products panel quietly reports a null price.
     */
    @Test
    @DisplayName("a Money field on an aggregation projection is populated from the stored double")
    void moneyIsMappedIntoAnAggregationProjection() {
        mongoTemplate.getCollection("products")
                .insertOne(new Document("_id", new ObjectId(PRODUCT_HEX))
                        .append("title", "RTX 4090")
                        .append("price", 1599.0));

        BestSellerDTO projected = mongoTemplate.aggregate(
                        Aggregation.newAggregation(
                                Aggregation.project()
                                        .and("_id").as("productId")
                                        .and("title").as("title")
                                        .and("price").as("price")),
                        "products", BestSellerDTO.class)
                .getMappedResults()
                .getFirst();

        assertThat(projected.getPrice()).isEqualTo(Money.of(1599.0));
        assertThat(projected.getProductId()).isEqualTo(PRODUCT_HEX);
    }

    /**
     * Queries built from value objects have to reach the same documents as the raw values
     * they replace, or every repository method silently returns nothing.
     */
    @Test
    @DisplayName("a criteria built from a value object matches the stored document")
    void valueObjectsAreUsableInQueries() {
        Product product = ProductFixture.aProduct()
                .titled("RTX 4090")
                .pricedAt(1599.0)
                .inCondition(ProductCondition.LIKE_NEW)
                .inCategories(CategoryId.of(CATEGORY_HEX))
                .build();
        mongoTemplate.save(product);

        assertThat(mongoTemplate.find(
                new Query(Criteria.where("condition").is(ProductCondition.LIKE_NEW)), Product.class))
                .hasSize(1);
        assertThat(mongoTemplate.find(
                new Query(Criteria.where("categoryIds").in(List.of(CategoryId.of(CATEGORY_HEX)))),
                Product.class))
                .hasSize(1);
        assertThat(mongoTemplate.find(
                new Query(Criteria.where("price").is(Money.of(1599.0))), Product.class))
                .hasSize(1);
    }
}
