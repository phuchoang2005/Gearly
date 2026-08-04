package com.dominator.gearly.ordering.infrastructure;

import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderPage;
import com.dominator.gearly.ordering.domain.OrderQuery;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.ShippingInformation;
import com.dominator.gearly.shared.domain.Address;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order repository adapter, against a real MongoDB.
 *
 * <p>The customer order search had <b>no coverage at all</b> before this. That mattered more
 * than usual in S10, because the three repository methods behind it — no filter, status only,
 * free text — were collapsed into one query and the aggregate's fields changed type
 * underneath them at the same time. A typed {@code UserId} that reached the criteria builder
 * unwrapped, or a status matched as an enum where the document holds a string, would return
 * an empty list rather than fail, and no unit test with a mocked repository could see it.
 *
 * <p>Docker-gated, so {@code mvn test} still passes offline.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MongoOrderRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @Autowired private OrderRepository orderRepository;
    @Autowired private MongoTemplate mongoTemplate;

    private static final UserId ADA = UserId.of("user-ada");
    private static final UserId GRACE = UserId.of("user-grace");

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(Order.class);
    }

    // ---- fixtures ----------------------------------------------------------

    private Order save(UserId owner, OrderStatus status, ShippingInformation shipping, double price) {
        Order order = OrderFixture.anOrder()
                .ownedBy(owner.value())
                .withLines(OrderFixture.line("p1", "RTX 4090", price, 1))
                .build();
        Order saved = orderRepository.save(order);
        for (OrderStatus step : OrderFixture.pathTo(status)) {
            saved.transitionTo(step);
        }
        if (shipping != null) {
            saved.amend(null, shipping, null, null, null, OrderFixture.PRICING);
        }
        return orderRepository.save(saved);
    }

    private Order save(UserId owner, OrderStatus status) {
        return save(owner, status, null, 100.00);
    }

    private OrderQuery query(UserId userId, OrderStatus status, String term) {
        return new OrderQuery(userId, status, term, 0, 10);
    }

    // ---- scoping -----------------------------------------------------------

    @Test
    @DisplayName("a query only ever returns the querying customer's own orders")
    void neverLeaksAnotherCustomersOrders() {
        save(ADA, OrderStatus.PENDING);
        save(ADA, OrderStatus.PROCESSING);
        save(GRACE, OrderStatus.PENDING);

        OrderPage page = orderRepository.findFor(query(ADA, null, null));

        assertThat(page.totalElements()).isEqualTo(2);
        assertThat(page.content()).allSatisfy(order -> assertThat(order.isOwnedBy(ADA)).isTrue());
    }

    /**
     * The regression a mocked repository cannot see: {@code UserId} is a record, and a record
     * reaching {@code Criteria.where("userId").is(...)} unconverted would match nothing.
     */
    @Test
    @DisplayName("the typed UserId is unwrapped before it reaches the query")
    void matchesTheStoredStringUserId() {
        save(ADA, OrderStatus.PENDING);

        assertThat(orderRepository.findFor(query(ADA, null, null)).totalElements()).isEqualTo(1);
        assertThat(orderRepository.countByUserAndStatus(ADA, OrderStatus.PENDING)).isEqualTo(1);
    }

    // ---- filtering ---------------------------------------------------------

    @Nested
    @DisplayName("status filter")
    class StatusFilter {

        @Test
        void matchesTheStoredStringForm() {
            save(ADA, OrderStatus.PENDING);
            save(ADA, OrderStatus.PROCESSING);
            save(ADA, OrderStatus.PROCESSING);

            assertThat(orderRepository.findFor(query(ADA, OrderStatus.PROCESSING, null)).totalElements())
                    .isEqualTo(2);
            assertThat(orderRepository.findFor(query(ADA, OrderStatus.CANCELLED, null)).content())
                    .isEmpty();
        }

        @Test
        void countsPerStatusAndExcludingTheFinalOnes() {
            save(ADA, OrderStatus.PENDING);
            save(ADA, OrderStatus.PROCESSING);
            save(ADA, OrderStatus.CANCELLED);
            save(GRACE, OrderStatus.PENDING);

            assertThat(orderRepository.countByUserAndStatus(ADA, OrderStatus.PENDING)).isEqualTo(1);
            assertThat(orderRepository.countByUserAndStatusNotIn(
                    ADA, List.of(OrderStatus.CANCELLED, OrderStatus.COMPLETED, OrderStatus.REFUNDED)))
                    .as("the two in-progress orders, not the cancelled one")
                    .isEqualTo(2);
        }
    }

    // ---- free-text search --------------------------------------------------

    @Nested
    @DisplayName("free-text search")
    class Search {

        private ShippingInformation to(String firstName, String lastName, String city) {
            Address address = new Address("1 Main St", city, 1, "State", 2, "0000", "Country", 3);
            return new ShippingInformation(firstName, lastName,
                    firstName.toLowerCase() + "@example.com", "0123456789", address);
        }

        @Test
        void findsByRecipientName() {
            save(ADA, OrderStatus.PENDING, to("Ada", "Lovelace", "London"), 100.00);
            save(ADA, OrderStatus.PENDING, to("Grace", "Hopper", "New York"), 100.00);

            assertThat(orderRepository.findFor(query(ADA, null, "lovelace")).totalElements())
                    .as("case-insensitive on the surname")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("a full name matches across the two name fields, in either order")
        void findsByFullNameInEitherOrder() {
            save(ADA, OrderStatus.PENDING, to("Ada", "Lovelace", "London"), 100.00);

            assertThat(orderRepository.findFor(query(ADA, null, "Ada Lovelace")).totalElements()).isEqualTo(1);
            assertThat(orderRepository.findFor(query(ADA, null, "Lovelace Ada")).totalElements()).isEqualTo(1);
        }

        @Test
        void findsByCityAndByLineTitle() {
            save(ADA, OrderStatus.PENDING, to("Ada", "Lovelace", "London"), 100.00);

            assertThat(orderRepository.findFor(query(ADA, null, "London")).totalElements()).isEqualTo(1);
            assertThat(orderRepository.findFor(query(ADA, null, "RTX")).totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("a numeric term also matches the order total")
        void findsByTotal() {
            Order order = save(ADA, OrderStatus.PENDING, to("Ada", "Lovelace", "London"), 100.00);

            String total = String.valueOf(order.getTotalAmount().toDouble());
            assertThat(orderRepository.findFor(query(ADA, null, total)).totalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("the order id is an exact match, not a substring one")
        void findsByOrderId() {
            Order order = save(ADA, OrderStatus.PENDING);
            save(ADA, OrderStatus.PENDING);

            assertThat(orderRepository.findFor(query(ADA, null, order.getId())).content())
                    .singleElement()
                    .extracting(Order::getId)
                    .isEqualTo(order.getId());
        }

        @Test
        @DisplayName("a term that matches nothing returns an empty page, not everything")
        void anUnmatchedTermReturnsNothing() {
            save(ADA, OrderStatus.PENDING, to("Ada", "Lovelace", "London"), 100.00);

            assertThat(orderRepository.findFor(query(ADA, null, "Babbage")).content()).isEmpty();
        }

        @Test
        @DisplayName("a search term is combined with the status filter, not substituted for it")
        void combinesWithTheStatusFilter() {
            save(ADA, OrderStatus.PENDING, to("Ada", "Lovelace", "London"), 100.00);
            save(ADA, OrderStatus.PROCESSING, to("Ada", "Lovelace", "London"), 100.00);

            assertThat(orderRepository.findFor(query(ADA, OrderStatus.PROCESSING, "Lovelace")).totalElements())
                    .isEqualTo(1);
        }
    }

    // ---- paging ------------------------------------------------------------

    @Test
    @DisplayName("paging reports the full count, not the size of the slice")
    void pagesWithTheTotalCount() {
        for (int i = 0; i < 5; i++) {
            save(ADA, OrderStatus.PENDING);
        }

        OrderPage first = orderRepository.findFor(new OrderQuery(ADA, null, null, 0, 2));
        OrderPage last = orderRepository.findFor(new OrderQuery(ADA, null, null, 2, 2));

        assertThat(first.content()).hasSize(2);
        assertThat(first.totalElements()).isEqualTo(5);
        assertThat(last.content()).hasSize(1);
        assertThat(last.totalElements()).isEqualTo(5);
    }

    // ---- round trip --------------------------------------------------------

    @Test
    @DisplayName("an order survives the round trip through the adapter with its behavior intact")
    void reloadsAsAWorkingAggregate() {
        Order saved = save(ADA, OrderStatus.PENDING);

        Order reloaded = orderRepository.findById(OrderId.of(saved.getId())).orElseThrow();

        assertThat(reloaded.isOwnedBy(ADA)).isTrue();
        assertThat(reloaded.getTotalAmount()).isEqualTo(saved.getTotalAmount());
        assertThat(reloaded.getItems()).singleElement()
                .extracting(line -> line.getProductId().value()).isEqualTo("p1");

        // the payment list read back from Mongo must accept an append, which is what the
        // gateway callback and the cancel path both do to a reloaded order
        reloaded.transitionTo(OrderStatus.PROCESSING);
        reloaded.recordPayment(com.dominator.gearly.ordering.domain.TransactionStatus.SUCCESSFUL, "settled");
        assertThat(reloaded.getPayment().getTransactions()).hasSize(2);
        assertThat(orderRepository.save(reloaded).getVersion()).isGreaterThan(saved.getVersion());
    }
}
