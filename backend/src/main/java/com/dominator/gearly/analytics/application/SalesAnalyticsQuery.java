package com.dominator.gearly.analytics.application;

import com.dominator.gearly.analytics.api.QuantitySoldDTO;
import com.dominator.gearly.analytics.api.TimeFrame;
import com.dominator.gearly.analytics.api.TopSellerDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.unwind;

/**
 * Read-only sales analytics over the orders collection.
 *
 * <p>Part of the explicit CQRS split S13 makes: this reads raw documents with an aggregation
 * pipeline and returns DTOs, and never loads, mutates or names an aggregate. That is what earns
 * {@code analytics} the codebase's only licence to use {@code MongoTemplate} outside a
 * repository adapter.
 *
 * <p>The price of the licence is stated in {@code analytics/package-info.java}: this class is
 * coupled to field names rather than to types, so nothing but an integration test against a
 * real MongoDB can tell you it still works. {@code SalesAnalyticsQueryIntegrationTest} is that
 * test.
 */
@RequiredArgsConstructor
@Service
public class SalesAnalyticsQuery {

    /**
     * The collection, by name.
     *
     * <p>It used to be {@code Order.class}. Passing the aggregate class made this the one place
     * in the read side that named a domain type — which
     * {@code contexts_touch_each_other_only_through_published_types} refuses, and rightly: the
     * whole claim of a query side is that it reads documents, so a change to the aggregate has
     * no business rippling into a report. There are no {@code @Field} renames on {@code Order},
     * so the emitted pipeline is byte-identical; {@code SalesAnalyticsQueryIntegrationTest}
     * runs against a real MongoDB and is what actually confirms that.
     */
    private static final String ORDERS = "orders";

    private final MongoTemplate mongoTemplate;

    public List<QuantitySoldDTO> getQuantitySold(TimeFrame timeFrame) {
        MatchOperation match = buildMatch(timeFrame);

        GroupOperation group = group("items.productId")
                .first("items.title").as("title")
                .sum("items.quantity").as("totalSold");

        ProjectionOperation project = Aggregation.project()
                .and("_id").as("productId")
                .and("title").as("title")
                .and("totalSold").as("totalSold")
                .andExclude("_id");

        Aggregation agg = newAggregation(
                match,
                unwind("items"),
                group,
                project,
                sort(Sort.by(Sort.Direction.DESC, "totalSold"))
        );
        return mongoTemplate
                .aggregate(agg, ORDERS, QuantitySoldDTO.class)
                .getMappedResults();
    }

    public List<TopSellerDTO> getTop5BestSelling(TimeFrame timeFrame) {
        MatchOperation match = buildMatch(timeFrame);
        UnwindOperation unwind = unwind("items");

        GroupOperation group = group("items.productId")
                .sum("items.quantity").as("totalSold")
                .first("items.title").as("title");

        SortOperation sort = sort(Sort.by(Sort.Direction.DESC, "totalSold"));
        LimitOperation limit = limit(5);

        ProjectionOperation project = project()
                .and("_id").as("productId")
                .and("totalSold").as("totalSold")
                .and("title").as("title")
                .andExclude("_id");

        Aggregation agg = newAggregation(match, unwind, group, sort, limit, project);

        return mongoTemplate
                .aggregate(agg, ORDERS, TopSellerDTO.class)
                .getMappedResults();
    }

    private MatchOperation buildMatch(TimeFrame timeFrame) {
        Criteria criteria = Criteria.where("orderStatus").is("COMPLETED");
        Instant start = timeFrame.getStartInstant();
        if (start != null) {
            criteria = criteria.and("doneAt").gte(start);
        }
        return match(criteria);
    }
}
