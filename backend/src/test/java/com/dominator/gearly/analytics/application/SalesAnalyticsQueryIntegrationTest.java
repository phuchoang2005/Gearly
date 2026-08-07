package com.dominator.gearly.analytics.application;

import com.dominator.gearly.analytics.api.QuantitySoldDTO;
import com.dominator.gearly.analytics.api.TopSellerDTO;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.analytics.api.TimeFrame;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

/**
 * Integration coverage for the sales-analytics aggregations against a real MongoDB
 * (Testcontainers). Guards two things the aggregation depends on: the {@code orderStatus}
 * document field name and value ("COMPLETED"), and the {@link TimeFrame} {@code doneAt}
 * lower bound. Docker-gated so {@code mvn test} still passes offline.
 */
@DataMongoTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SalesAnalyticsQueryIntegrationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6.0"));

    @Autowired
    private MongoTemplate mongoTemplate;

    private SalesAnalyticsQuery service;

    private final Instant now = Instant.now();

    @BeforeEach
    void setUp() {
        service = new SalesAnalyticsQuery(mongoTemplate);
        mongoTemplate.dropCollection(Order.class);

        // p1: 2 + 3 = 5, recent COMPLETED
        save(OrderStatus.COMPLETED, daysAgo(2), item("p1", "GPU", 2));
        save(OrderStatus.COMPLETED, daysAgo(2), item("p1", "GPU", 3));
        // p2: 1, recent COMPLETED
        save(OrderStatus.COMPLETED, daysAgo(3), item("p2", "CPU", 1));
        // p3: 50, COMPLETED but a year old
        save(OrderStatus.COMPLETED, daysAgo(400), item("p3", "PSU", 50));
        // noise: a big PENDING order must never be counted
        save(OrderStatus.PENDING, daysAgo(1), item("p1", "GPU", 100));
    }

    @Test
    void quantitySold_all_countsOnlyCompleted_sortedDesc() {
        List<QuantitySoldDTO> result = service.getQuantitySold(TimeFrame.ALL);

        assertThat(result)
                .extracting(QuantitySoldDTO::getProductId, QuantitySoldDTO::getTotalSold)
                .containsExactly(          // sorted by totalSold desc
                        tuple("p3", 50L),
                        tuple("p1", 5L),
                        tuple("p2", 1L));
    }

    @Test
    void quantitySold_oneMonth_excludesOrdersOlderThanTheWindow() {
        List<QuantitySoldDTO> result = service.getQuantitySold(TimeFrame.ONE_MONTH);

        // p3 (400 days old) drops out; the recent COMPLETED orders remain
        assertThat(result)
                .extracting(QuantitySoldDTO::getProductId, QuantitySoldDTO::getTotalSold)
                .containsExactly(
                        tuple("p1", 5L),
                        tuple("p2", 1L));
    }

    @Test
    void top5BestSelling_all_returnsBestFirst() {
        List<TopSellerDTO> result = service.getTop5BestSelling(TimeFrame.ALL);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getProductId()).isEqualTo("p3");
        assertThat(result.get(0).getTotalSold()).isEqualTo(50L);
        assertThat(result).extracting(TopSellerDTO::getProductId)
                .containsExactly("p3", "p1", "p2");
    }

    // --- helpers ---------------------------------------------------------------

    private Instant daysAgo(int days) {
        return now.minus(days, ChronoUnit.DAYS);
    }

    private Document item(String productId, String title, int quantity) {
        return new Document()
                .append("productId", productId)
                .append("title", title)
                .append("quantity", quantity)
                .append("price", 10.0);
    }

    /**
     * Inserts the raw document rather than saving an {@code Order}.
     *
     * <p>Deliberate, and better than what it replaces. These aggregations run against stored
     * BSON, never against the mapped aggregate, so the thing worth pinning is the document
     * shape — the {@code orderStatus} field holding the string {@code "COMPLETED"}, the
     * {@code items} array, the {@code doneAt} date. Writing through the aggregate would put
     * the write model between the test and the shape it is meant to assert, and would also
     * mean walking every fixture through four legal status transitions to reach
     * {@code COMPLETED} just to produce a document this states in one line.
     */
    private void save(OrderStatus status, Instant doneAt, Document... items) {
        mongoTemplate.getCollection("orders").insertOne(new Document()
                .append("userId", "u1")
                .append("items", List.of(items))
                .append("orderStatus", status.name())
                .append("doneAt", Date.from(doneAt))
                .append("totalAmount", 0.0)
                .append("version", 0L));
    }
}
