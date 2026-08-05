package com.dominator.gearly.ordering.application;

import com.dominator.gearly.cart.domain.Cart;
import com.dominator.gearly.catalog.domain.Image;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.catalog.domain.Product;
import com.dominator.gearly.ordering.domain.ShippingInformation;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.catalog.domain.ProductRepository;
import com.dominator.gearly.cart.application.CartService;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.ProductCondition;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import com.dominator.gearly.shared.domain.UserId;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

    @Autowired private PlaceOrderService placeOrderService;
    @Autowired private ProductRepository productRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private MongoTemplate mongoTemplate;

    /**
     * The injection point for the mid-flow failure. {@code CatalogStockListener} decrements
     * stock and {@code CartOrderListener} clears the cart, both {@code BEFORE_COMMIT}, so
     * making the cart clear blow up puts the failure strictly after a successful stock write —
     * exactly the window the transaction has to cover.
     */
    @MockitoBean private CartService cartService;

    private static final String USER_ID = "user-under-test";

    @BeforeEach
    void setUp() {
        // Dropping the collection rather than calling a repository delete: the OrderRepository
        // port deliberately has no deleteAll — nothing in the application ever deletes an
        // order, and a port should not grow a method purely so a test can use it.
        mongoTemplate.dropCollection(Order.class);
        mongoTemplate.dropCollection(Product.class);
        mongoTemplate.dropCollection(Cart.class);

        // Re-create them straight away. Implicitly creating a collection is a DDL operation,
        // and two transactions that both try it at once lose to a WriteConflict — which is
        // what the concurrency tests below were measuring before this line existed, rather
        // than anything about stock. Found by writing a positive control that failed.
        mongoTemplate.createCollection(Order.class);
        mongoTemplate.createCollection(Product.class);
        mongoTemplate.createCollection(Cart.class);
    }

    // ---- fixtures ----------------------------------------------------------

    private Product savedProductWithStock(int stock) {
        Product product = Product.create("RTX 4090", List.of("NVIDIA"), null,
                Money.of(1599.00), Money.of(1599.00), ProductCondition.NEW, Quantity.of(stock),
                null, List.of(new Image("http://img/gpu.png", "gpu")));
        return productRepository.save(product);
    }

    private PlaceOrderCommand orderFor(String productId, int quantity) {
        return new PlaceOrderCommand(
                List.of(new PlaceOrderCommand.RequestedLine(productId, quantity)),
                "cod",
                new ShippingInformation("Ada", "Lovelace", "ada@example.com", "0123456789", null));
    }

    private int stockOf(String productId) {
        return productRepository.findById(ProductId.of(productId)).orElseThrow().getStock().toInt();
    }

    // ---- 1. transactional rollback -----------------------------------------

    @Test
    @DisplayName("a failure after the stock decrement rolls back BOTH the order and the stock")
    void failureMidPlacement_rollsBackOrderAndStock() {
        Product product = savedProductWithStock(10);
        doThrow(new IllegalStateException("cart service exploded"))
                .when(cartService).removeItems(any(), any(), anyMap());

        assertThatThrownBy(() -> placeOrderService.place(UserId.of(USER_ID), orderFor(product.getId(), 3)))
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

        Order order = placeOrderService.place(UserId.of(USER_ID), orderFor(product.getId(), 3));

        assertThat(order.getId()).isNotNull();
        assertThat(orderRepository.findById(order.orderId())).isPresent();
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

        Product checkoutA = productRepository.findById(product.productId()).orElseThrow();
        Product checkoutB = productRepository.findById(product.productId()).orElseThrow();
        assertThat(checkoutA.getVersion()).isEqualTo(checkoutB.getVersion());

        checkoutA.reserve(Quantity.ONE);
        productRepository.save(checkoutA);

        checkoutB.reserve(Quantity.ONE);
        assertThatThrownBy(() -> productRepository.save(checkoutB))
                .isInstanceOf(OptimisticLockingFailureException.class);

        assertThat(stockOf(product.getId())).isZero();
    }

    /**
     * <b>The S11 verify step: a concurrent checkout cannot oversell.</b>
     *
     * <p>The test above this one demonstrates the mechanism by driving two writes to the same
     * document by hand. This one drives two <em>real placements</em> — through
     * {@code PlaceOrderService}, the transaction, and {@code CatalogStockListener} — for the
     * last unit in stock, and asserts the outcome that actually matters to a customer: one
     * order exists, one placement failed, and the shelf is empty rather than negative.
     *
     * <p>It does not assert <em>which</em> exception the loser gets. Under a real replica set
     * the second transaction may lose to Spring Data's {@code @Version} check or to MongoDB's
     * own write conflict depending on how the two interleave, and pinning one of those would
     * make the test a description of a race's timing rather than of its guarantee.
     */
    @Test
    @DisplayName("two concurrent checkouts for the last unit: one wins, one fails, nothing oversells")
    void concurrentCheckouts_cannotOversell() throws Exception {
        Product product = savedProductWithStock(1);

        PlaceOrderCommand order = orderFor(product.getId(), 1);
        long winners = runConcurrently(
                new Checkout(UserId.of("buyer-a"), order),
                new Checkout(UserId.of("buyer-b"), order));

        assertThat(winners).as("exactly one checkout may take the last unit").isEqualTo(1);
        assertThat(stockOf(product.getId())).as("stock must never go negative").isZero();
        assertThat(orderRepository.findAll()).as("one order, not two").hasSize(1);
    }

    /**
     * <b>The positive control</b>, and it earned its keep immediately.
     *
     * <p>Without it, "exactly one winner" above could be true because the second checkout
     * always fails for a reason having nothing to do with stock — a misconfigured pool, a
     * latch that never releases, an exception thrown before either transaction opens. Two
     * genuinely independent checkouts both commit, so both are really being attempted and the
     * harness is measuring what it claims to measure.
     *
     * <p>Independent means different customers <em>and</em> different products: a placement
     * writes an order, a product and the buyer's cart, so two checkouts sharing any of the
     * three share a document to conflict on.
     */
    @Test
    @DisplayName("two concurrent checkouts by different customers for different products: both succeed")
    void concurrentCheckouts_bothSucceedWhenIndependent() throws Exception {
        Product gpu = savedProductWithStock(5);
        Product cpu = savedProductWithStock(5);

        assertThat(runConcurrently(
                new Checkout(UserId.of("buyer-a"), orderFor(gpu.getId(), 1)),
                new Checkout(UserId.of("buyer-b"), orderFor(cpu.getId(), 1))))
                .as("independent sales must not interfere")
                .isEqualTo(2);
        assertThat(orderRepository.findAll()).hasSize(2);
    }

    /**
     * <b>What the first version of the positive control actually found, recorded rather than
     * deleted.</b>
     *
     * <p>The obvious control — two concurrent checkouts of the <em>same</em> product with
     * stock for both — does not pass, and should not be expected to. Optimistic locking is
     * conservative: both checkouts read the same product document, both write it, and the
     * second write is rejected on the version whether or not the stock covered it. There is no
     * retry anywhere in the system, so the loser gets a 409.
     *
     * <p>That is the documented contract — {@code GlobalExceptionHandler} answers "This item
     * was modified by another request. Please refresh and try again." — and it is the correct
     * trade for a single-document stock counter: refusing a sale that would have been fine is
     * recoverable, and overselling is not. It is worth knowing, though, because it means a
     * popular product under simultaneous load will turn away checkouts that had stock. A retry
     * on {@code OptimisticLockingFailureException} would close that, and it belongs to whoever
     * owns throughput rather than to a refactoring sprint.
     */
    @Test
    @DisplayName("two concurrent checkouts of the SAME product conflict even when stock allows — no retry exists")
    void concurrentCheckouts_ofTheSameProductConflictEvenWithStock() throws Exception {
        Product product = savedProductWithStock(5);
        PlaceOrderCommand order = orderFor(product.getId(), 1);

        assertThat(runConcurrently(
                new Checkout(UserId.of("buyer-a"), order),
                new Checkout(UserId.of("buyer-b"), order)))
                .as("the loser is refused on the version, not on the stock")
                .isEqualTo(1);
        assertThat(stockOf(product.getId()))
                .as("and the refused sale took nothing")
                .isEqualTo(4);
    }

    /** One customer placing one order. */
    private record Checkout(UserId buyer, PlaceOrderCommand order) {
    }

    /** Fires two placements at the same instant and reports how many committed. */
    private long runConcurrently(Checkout first, Checkout second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch bothReady = new CountDownLatch(2);

        List<Callable<Boolean>> checkouts = List.of(first, second).stream()
                .map(checkout -> (Callable<Boolean>) () -> {
                    bothReady.countDown();
                    bothReady.await(5, TimeUnit.SECONDS);
                    try {
                        placeOrderService.place(checkout.buyer(), checkout.order());
                        return true;
                    } catch (RuntimeException expectedForTheLoser) {
                        return false;
                    }
                })
                .toList();

        List<Future<Boolean>> results = pool.invokeAll(checkouts);
        pool.shutdown();

        long winners = 0;
        for (Future<Boolean> result : results) {
            if (result.get()) {
                winners++;
            }
        }
        return winners;
    }

    @Test
    @DisplayName("a normal save bumps the version")
    void save_incrementsTheVersion() {
        Product product = savedProductWithStock(5);
        assertThat(product.getVersion()).isZero();

        product.reserve(Quantity.ONE);
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

        Product loaded = productRepository.findById(ProductId.of("legacy-product")).orElseThrow();
        assertThat(loaded.getVersion()).isZero();

        loaded.reserve(Quantity.ONE);
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

        Product loaded = productRepository.findById(ProductId.of("seeded-product")).orElseThrow();
        loaded.reserve(Quantity.ONE);
        productRepository.save(loaded);

        assertThat(productRepository.findAll()).hasSize(1);
        assertThat(stockOf("seeded-product")).isEqualTo(8);
    }
}
