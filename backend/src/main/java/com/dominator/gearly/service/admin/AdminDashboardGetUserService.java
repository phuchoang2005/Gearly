package com.dominator.gearly.service.admin;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import com.dominator.gearly.dto.LoyalCustomerDTO;
import com.dominator.gearly.dto.TopAvgOrderValueUserDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardGetUserService {
    private final MongoTemplate mongoTemplate;

    public List<LoyalCustomerDTO> getTop10LoyalCustomers() {

        /* Step 1: GROUP by userId (String) */
        GroupOperation groupByUser = Aggregation.group("userId")
                .count().as("totalOrders")
                .sum("totalAmount").as("totalSpending")
                .min("addedAt").as("firstOrder")
                .max("addedAt").as("lastOrder");

        /* Step 2: SORT by total spending & LIMIT 10 */
        SortOperation sortBySpending = Aggregation.sort(Sort.Direction.DESC, "totalSpending");
        LimitOperation limit10 = Aggregation.limit(10);

        /* Step 3: LOOKUP into users, converting _id -> String to compare */
        // Use a pipeline-form $lookup because we need $toString
        AggregationOperation lookupUsers = ctx -> new Document("$lookup",
                new Document("from", "users")
                        .append("let", Map.of("uid", "$_id")) // userId is a String
                        .append("pipeline", List.of(
                                new Document("$match",
                                        new Document("$expr",
                                                new Document("$eq", List.of(
                                                        new Document("$toString", "$_id"), // convert ObjectId -> String
                                                        "$$uid")))),
                                new Document("$project",
                                        new Document("_id", 0)
                                                .append("fullName", 1)
                                                .append("email", 1)
                                                .append("phone", 1))))
                        .append("as", "user"));

        /* Step 4: UNWIND & PROJECT */
        UnwindOperation unwindUser = Aggregation.unwind("user");

        ProjectionOperation project = Aggregation.project()
                .and("_id").as("userId")
                .and("user.fullName").as("fullName")
                .and("user.email").as("email")
                .and("user.phone").as("phone")
                .andInclude("totalOrders", "totalSpending", "firstOrder", "lastOrder");

        /* Build pipeline */
        Aggregation pipeline = Aggregation.newAggregation(
                groupByUser,
                sortBySpending,
                limit10,
                lookupUsers,
                unwindUser,
                project);

        return mongoTemplate
                .aggregate(pipeline, "orders", LoyalCustomerDTO.class)
                .getMappedResults();
    }

    public TopAvgOrderValueUserDTO findUserWithHighestAvgOrderValue() {
        /*
         * Step 1: GROUP by userId (String) and compute the average value, order count, and total spend
         */
        GroupOperation groupByUser = Aggregation.group("userId")
                .avg("totalAmount").as("avgOrderValue")
                .sum("totalAmount").as("totalSpent")
                .count().as("totalOrders");

        /* Step 2: SORT by avgOrderValue descending and LIMIT 1 */
        SortOperation sortDesc = Aggregation.sort(
                Sort.Direction.DESC, "avgOrderValue");
        LimitOperation limit1 = Aggregation.limit(1);

        /* Step 3: LOOKUP into users by casting _id -> String and comparing with userId */
        AggregationOperation lookupUser = ctx -> new Document("$lookup",
                new Document("from", "users")
                        .append("let", Map.of("uid", "$_id")) // _id here is the userId (String)
                        .append("pipeline", List.of(
                                new Document("$match",
                                        new Document("$expr",
                                                new Document("$eq",
                                                        List.of(
                                                                new Document("$toString", "$_id"),
                                                                "$$uid")))),
                                new Document("$project",
                                        new Document("_id", 0)
                                                .append("fullName", 1)
                                                .append("email", 1))))
                        .append("as", "user"));

        UnwindOperation unwindUser = Aggregation.unwind("user");

        /* Step 4: PROJECT into the DTO shape */
        ProjectionOperation project = Aggregation.project()
                .and("_id").as("userId")
                .and("user.fullName").as("fullName")
                .and("user.email").as("email")
                .and("avgOrderValue").as("averageOrderValue");

        /* Build pipeline */
        Aggregation pipeline = Aggregation.newAggregation(
                groupByUser,
                sortDesc,
                limit1,
                lookupUser,
                unwindUser,
                project);

        /* Execute and map the result */
        AggregationResults<TopAvgOrderValueUserDTO> results = mongoTemplate.aggregate(pipeline, "orders",
                TopAvgOrderValueUserDTO.class);

        return results.getUniqueMappedResult(); // may return null if there are no orders
    }
}
