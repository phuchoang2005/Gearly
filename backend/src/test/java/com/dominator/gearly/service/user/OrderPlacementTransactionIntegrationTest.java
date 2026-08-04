package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.OrderCreationRequestDTO;
import com.dominator.gearly.dto.OrderItemRequestDTO;
import com.dominator.gearly.dto.PaymentRequestDTO;
import com.dominator.gearly.model.Image;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.ordering.domain.ShippingInformation;
import com.dominator.gearly.model.User;
import com.dominator.gearly.repository.OrderRepository;
import com.dominator.gearly.repository.ProductRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import com.dominator.gearly.shared.domain.Money;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doThrow;

/**
 * Proves the two S8 consistency guarantees against a <b>real</b> MongoDB, because neither
 * can be demonstrated with mocks:
 *
 * <ol>
 *   <li><b>Transactions are real.</b> Before S8 there was no {@code MongoTransactionManager}
 *       bean, so all seven {@code @Transactional} annotations were decoration. A failure
 *       partway through {@code createOrder} left the order saved and the stock decremented
 *       against an order that was never completed.</li>
 *   <li><b>Optimistic locking closes the oversell race.</b> Two checkouts could read the
 *       same stock figure, both pass the availability check, and both write — selling one
 *       unit twice.</li>
 * </ol>
 *
 * <p>Testcontainers' {@code MongoDBContainer} always starts a single-node replica set,
 * which is what makes transactions available at all. Docker-gated
 * ({@code disabledWithoutDocker}) so {@code mvn test} still passes offline — run with
 * Colima up to actually exercise these.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class OrderPlacementTransactionIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @Autowired private CustomerOrderService customerOrderService;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private MongoTemplate mongoTemplate;

    /**
     * The injection point for the mid-flow failure. {@code applyStockAndClearCart}
     * decrements stock first and clears the cart second, so making the cart clear blow up
     * puts the failure strictly after a successful stock write — exactly the window the
     * transaction has to cover.
     */
    @MockitoBean private CartService cartService;

    private static final String USER_ID = "user-under-test";

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        productRepository.deleteAll();
    }

    // ---- fixtures ----------------------------------------------------------

    private Product savedProductWithStock(int stock) {
        Product product = new Product();
        product.setTitle("RTX 4090");
        product.setPrice(Money.of(1599.00));
        product.setStock(stock);
        product.setImages(List.of(new Image("http://img/gpu.png", "gpu")));
        return productRepository.save(product);
    }

    private AuthenticatedUser authUser() {
        User user = new User();
        user.setId(USER_ID);
        return new AuthenticatedUser(user);
    }

    private OrderCreationRequestDTO orderFor(String productId, int quantity) {
        OrderItemRequestDTO item = new OrderItemRequestDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);

        PaymentRequestDTO payment = new PaymentRequestDTO();
        payment.setMethod("cod");

        OrderCreationRequestDTO request = new OrderCreationRequestDTO();
        request.setItems(List.of(item));
        request.setPaymentInfo(payment);
        request.setShippingInformation(new ShippingInformation(
                "Ada", "Lovelace", "ada@example.com", "0123456789", null));
        return request;
    }

    private int stockOf(String productId) {
        return productRepository.findById(productId).orElseThrow().getStock();
    }

    // ---- 1. transactional rollback -----------------------------------------

    @Test
    @DisplayName("a failure after the stock decrement rolls back BOTH the order and the stock")
    void failureMidPlacement_rollsBackOrderAndStock() {
        Product product = savedProductWithStock(10);
        doThrow(new IllegalStateException("cart service exploded"))
                .when(cartService).removeItems(any(), any(), anyMap());

        assertThatThrownBy(() -> customerOrderService.createOrder(authUser(), orderFor(product.getId(), 3)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(orderRepository.findAll())
                .as("the order must not survive a failed placement")
                .isEmpty();
        assertThat(stockOf(product.getId()))
                .as("the stock decrement must be rolled back with it")
                .isEqualTo(10);
    }

    /**
     * The positive control. Without it the rollback test above could pass for the wrong
     * reason — because nothing was ever written in the first place.
     */
    @Test
    @DisplayName("a successful placement commits both the order and the stock decrement")
    void successfulPlacement_commitsOrderAndStock() {
        Product product = savedProductWithStock(10);

        Order order = customerOrderService.createOrder(authUser(), orderFor(product.getId(), 3));

        assertThat(order.getId()).isNotNull();
        assertThat(orderRepository.findById(order.getId())).isPresent();
        assertThat(stockOf(product.getId())).isEqualTo(7);
    }

    // ---- 2. optimistic locking ---------------------------------------------

    @Test
    @DisplayName("the second of two concurrent writers to the same product is rejected, not silently applied")
    void concurrentStockWrites_loseTheRace() {
        // The oversell race, made deterministic: two readers take the same snapshot, both
        // decide the sale is fine, and both try to write. Before @Version the second write
        // simply overwrote the first and one unit was sold twice.
        Product product = savedProductWithStock(1);

        Product checkoutA = productRepository.findById(product.getId()).orElseThrow();
        Product checkoutB = productRepository.findById(product.getId()).orElseThrow();
        assertThat(checkoutA.getVersion()).isEqualTo(checkoutB.getVersion());

        checkoutA.setStock(0);
        productRepository.save(checkoutA);

        checkoutB.setStock(0);
        assertThatThrownBy(() -> productRepository.save(checkoutB))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(stockOf(product.getId())).isZero();
    }

    @Test
    @DisplayName("a normal save bumps the version")
    void save_incrementsTheVersion() {
        Product product = savedProductWithStock(5);
        assertThat(product.getVersion()).isZero();

        product.setStock(4);
        Product updated = productRepository.save(product);

        assertThat(updated.getVersion()).isEqualTo(1L);
    }

    // ---- 3. the migrate.js backfill ----------------------------------------

    @Test
    @DisplayName("a pre-S8 document without a version field is updatable once migrate.js backfills 0")
    void versionBackfill_makesLegacyDocumentsUpdatable() {
        // Guards step 6 of data/seed/migrate.js. Spring Data reads a missing version as
        // null and treats a null version as "not yet persisted", so without the backfill
        // the first save() of a pre-S8 document is issued as an INSERT and dies on the
        // duplicate _id. This reproduces that document and the fix.
        Document legacy = new Document()
                .append("_id", "legacy-product")
                .append("title", "Pre-S8 GPU")
                .append("stock", 5);
        mongoTemplate.getCollection("products").insertOne(legacy);

        // exactly what migrate.js does
        mongoTemplate.getCollection("products").updateMany(
                new Document("version", new Document("$exists", false)),
                new Document("$set", new Document("version", 0L)));

        Product loaded = productRepository.findById("legacy-product").orElseThrow();
        assertThat(loaded.getVersion()).isZero();

        loaded.setStock(4);
        Product updated = productRepository.save(loaded);

        assertThat(updated.getVersion()).isEqualTo(1L);
        assertThat(productRepository.findAll()).hasSize(1);
        assertThat(stockOf("legacy-product")).isEqualTo(4);
    }

    /** The seed dumps carry {@code version: 0} for the same reason — see gearly.*.json. */
    @Test
    @DisplayName("a document that already carries version 0 is not re-inserted")
    void seededDocumentWithVersionZero_updatesInPlace() {
        Map<String, Object> seeded = Map.of(
                "_id", "seeded-product", "title", "Seeded GPU", "stock", 9, "version", 0L);
        mongoTemplate.getCollection("products").insertOne(new Document(seeded));

        Product loaded = productRepository.findById("seeded-product").orElseThrow();
        loaded.setStock(8);
        productRepository.save(loaded);

        assertThat(productRepository.findAll()).hasSize(1);
        assertThat(stockOf("seeded-product")).isEqualTo(8);
    }
}
