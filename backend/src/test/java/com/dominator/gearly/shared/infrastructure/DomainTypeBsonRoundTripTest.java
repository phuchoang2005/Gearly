package com.dominator.gearly.shared.infrastructure;

import com.dominator.gearly.model.Cart;
import com.dominator.gearly.model.CartItem;
import com.dominator.gearly.model.Order;
import com.dominator.gearly.model.OrderItem;
import com.dominator.gearly.model.Payment;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.Review;
import com.dominator.gearly.model.Transaction;
import com.dominator.gearly.model.TransactionStatus;
import com.dominator.gearly.model.User;
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
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

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
            Product product = new Product();
            product.setTitle("RTX 4090");
            product.setPrice(Money.of(109.99));
            product.setOriginalPrice(Money.of(120.5));
            mongoTemplate.save(product);

            Document raw = rawDocument("products");

            assertThat(raw.get("price")).isInstanceOf(Double.class).isEqualTo(109.99);
            assertThat(raw.get("originalPrice")).isInstanceOf(Double.class).isEqualTo(120.5);
        }

        @Test
        void onAnOrderTotalAndItsLines() {
            Order order = new Order();
            order.setUserId("u1");
            order.setTotalAmount(Money.of(3198.0));
            order.setItems(List.of(new OrderItem("p1", "GPU", Money.of(1599.0), "img", 2)));
            mongoTemplate.save(order);

            Document raw = rawDocument("orders");

            assertThat(raw.get("totalAmount")).isInstanceOf(Double.class).isEqualTo(3198.0);
            Document line = raw.getList("items", Document.class).getFirst();
            assertThat(line.get("price")).isInstanceOf(Double.class).isEqualTo(1599.0);
        }

        @Test
        void onAnEmbeddedPaymentTransaction() {
            Transaction tx = new Transaction();
            tx.setTransactionId("t1");
            tx.setStatus(TransactionStatus.PENDING);
            tx.setAmount(Money.of(36.60));

            Order order = new Order();
            order.setUserId("u1");
            order.setPayment(new Payment("momo", List.of(tx)));
            mongoTemplate.save(order);

            Document payment = rawDocument("orders").get("payment", Document.class);
            Document stored = payment.getList("transactions", Document.class).getFirst();

            assertThat(stored.get("amount")).isInstanceOf(Double.class).isEqualTo(36.6);
        }

        @Test
        void onACartLine() {
            CartItem item = new CartItem();
            item.setProductId("p1");
            item.setPrice(Money.of(24.99));
            item.setCondition(ProductCondition.LIKE_NEW);

            Cart cart = new Cart();
            cart.setUserId("u1");
            cart.setItems(List.of(item));
            mongoTemplate.save(cart);

            Document line = rawDocument("carts").getList("items", Document.class).getFirst();

            assertThat(line.get("price")).isInstanceOf(Double.class).isEqualTo(24.99);
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
    @DisplayName("ids keep the BSON type the collection already used")
    class IdStorage {

        /** Category ids are the one id stored as an ObjectId rather than a string. */
        @Test
        void categoryIdIsStoredAsAnObjectId() {
            Product product = new Product();
            product.setTitle("RTX 4090");
            product.setCategoryIds(List.of(CategoryId.of(CATEGORY_HEX)));
            mongoTemplate.save(product);

            Object stored = rawDocument("products").getList("categoryIds", Object.class).getFirst();

            assertThat(stored).isInstanceOf(ObjectId.class);
            assertThat(stored).isEqualTo(new ObjectId(CATEGORY_HEX));
        }

        /**
         * The asymmetry {@code ObjectIdBackedIdConverters} exists for: the same
         * {@code ProductId} type is an {@code ObjectId} here and a {@code String} on an
         * order line.
         */
        @Test
        void reviewIdsStayObjectIdsWhileOrderLineProductIdsStayStrings() {
            Review review = new Review();
            review.setProductId(ProductId.of(PRODUCT_HEX));
            review.setOrderId(OrderId.of(ORDER_HEX));
            review.setUserId(UserId.of(USER_HEX));
            review.setRating(5);
            mongoTemplate.save(review);

            Document rawReview = rawDocument("reviews");
            assertThat(rawReview.get("productId")).isInstanceOf(ObjectId.class)
                    .isEqualTo(new ObjectId(PRODUCT_HEX));
            assertThat(rawReview.get("orderId")).isInstanceOf(ObjectId.class);
            assertThat(rawReview.get("userId")).isInstanceOf(ObjectId.class);

            Order order = new Order();
            order.setUserId("u1");
            order.setItems(List.of(new OrderItem(PRODUCT_HEX, "GPU", Money.ZERO, "img", 1)));
            mongoTemplate.save(order);

            Document line = rawDocument("orders").getList("items", Document.class).getFirst();
            assertThat(line.get("productId")).isInstanceOf(String.class).isEqualTo(PRODUCT_HEX);
        }

        /**
         * A review written by S9 must still be findable by the queries that predate it —
         * the rating distribution aggregation matches on an {@code ObjectId} productId.
         */
        @Test
        void aReviewRemainsQueryableByRawObjectId() {
            Review review = new Review();
            review.setProductId(ProductId.of(PRODUCT_HEX));
            review.setUserId(UserId.of(USER_HEX));
            review.setRating(4);
            mongoTemplate.save(review);

            long matches = mongoTemplate.getCollection("reviews")
                    .countDocuments(new Document("productId", new ObjectId(PRODUCT_HEX)));

            assertThat(matches).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("enums keep their stored token")
    class EnumStorage {

        @Test
        void productConditionStoresTheSpacedWireValueNotTheConstantName() {
            Product product = new Product();
            product.setTitle("RTX 4090");
            product.setCondition(ProductCondition.LIKE_NEW);
            mongoTemplate.save(product);

            assertThat(rawDocument("products").get("condition"))
                    .isInstanceOf(String.class)
                    .isEqualTo("LIKE NEW");
        }

        @Test
        void roleStoresItsConstantName() {
            User user = new User();
            user.setEmail("ada@example.com");
            user.setRole(Role.ADMIN);
            mongoTemplate.save(user);

            assertThat(rawDocument("users").get("role")).isEqualTo("ADMIN");
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
     * The end-to-end shape check: save a fully populated product, then compare the raw
     * document field-for-field against the BSON types the pre-S9 seed dump holds.
     */
    @Test
    @DisplayName("a fully populated product document matches the pre-S9 field types exactly")
    void productDocumentShapeIsUnchanged() {
        Product product = new Product();
        product.setTitle("Intel Core i3-12100F");
        product.setAuthors(List.of("Intel"));
        product.setDescription("entry-level processor");
        product.setPrice(Money.of(109.99));
        product.setOriginalPrice(Money.of(120.5));
        product.setCondition(ProductCondition.NEW);
        product.setStock(64);
        product.setCategoryIds(List.of(CategoryId.of(CATEGORY_HEX)));
        product.setAverageRating(4.5);
        product.setRatingCount(12);
        product.setTotalRating(54);
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
     * Queries built from value objects have to reach the same documents as the raw values
     * they replace, or every repository method silently returns nothing.
     */
    @Test
    @DisplayName("a criteria built from a value object matches the stored document")
    void valueObjectsAreUsableInQueries() {
        Product product = new Product();
        product.setTitle("RTX 4090");
        product.setPrice(Money.of(1599.0));
        product.setCondition(ProductCondition.LIKE_NEW);
        product.setCategoryIds(List.of(CategoryId.of(CATEGORY_HEX)));
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
