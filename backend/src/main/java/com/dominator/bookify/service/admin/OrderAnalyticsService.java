package com.dominator.bookify.service.admin;

import com.dominator.bookify.dto.QuantitySoldDTO;
import com.dominator.bookify.dto.TopSellerDTO;
import com.dominator.bookify.model.Order;
import com.dominator.bookify.model.TimeFrame;
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
 * Read-only sales analytics over the orders collection (MongoDB aggregations),
 * extracted from the admin order service so CRUD/workflow and reporting stay
 * separate.
 */
@RequiredArgsConstructor
@Service
public class OrderAnalyticsService {

    private final MongoTemplate mongoTemplate;

    public List<QuantitySoldDTO> getQuantitySold(TimeFrame timeFrame) {
        MatchOperation match = buildMatch(timeFrame);

        GroupOperation group = group("items.bookId")
                .first("items.title").as("title")
                .sum("items.quantity").as("totalSold");

        ProjectionOperation project = Aggregation.project()
                .and("_id").as("bookId")
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
                .aggregate(agg, Order.class, QuantitySoldDTO.class)
                .getMappedResults();
    }

    public List<TopSellerDTO> getTop5BestSelling(TimeFrame timeFrame) {
        MatchOperation match = buildMatch(timeFrame);
        UnwindOperation unwind = unwind("items");

        GroupOperation group = group("items.bookId")
                .sum("items.quantity").as("totalSold")
                .first("items.title").as("title");

        SortOperation sort = sort(Sort.by(Sort.Direction.DESC, "totalSold"));
        LimitOperation limit = limit(5);

        ProjectionOperation project = project()
                .and("_id").as("bookId")
                .and("totalSold").as("totalSold")
                .and("title").as("title")
                .andExclude("_id");

        Aggregation agg = newAggregation(match, unwind, group, sort, limit, project);

        return mongoTemplate
                .aggregate(agg, Order.class, TopSellerDTO.class)
                .getMappedResults();
    }

    private MatchOperation buildMatch(TimeFrame timeFrame) {
        Criteria criteria = Criteria.where("orderstatus").is("COMPLETED");
        Instant start = timeFrame.getStartInstant();
        if (start != null) {
            criteria = criteria.and("doneAt").gte(start);
        }
        return match(criteria);
    }
}
