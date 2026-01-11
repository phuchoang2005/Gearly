package com.dominator.bookify.service.admin;

import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.dominator.bookify.dto.LoyalCustomerDTO;
import com.dominator.bookify.dto.TopAvgOrderValueUserDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardGetUserImp implements AdminDashboardGetUserInterface {
    @Autowired
    private final MongoTemplate mongoTemplate;

    @Override
    public List<LoyalCustomerDTO> getTop10LoyalCustomers() {

        /* B1: GROUP theo userId (String) */
        GroupOperation groupByUser = Aggregation.group("userId")
                .count().as("totalOrders")
                .sum("totalAmount").as("totalSpending")
                .min("addedAt").as("firstOrder")
                .max("addedAt").as("lastOrder");

        /* B2: SORT theo tổng chi tiêu & LIMIT 10 */
        SortOperation sortBySpending = Aggregation.sort(Sort.Direction.DESC, "totalSpending");
        LimitOperation limit10 = Aggregation.limit(10);

        /* B3: LOOKUP sang users, chuyển _id -> String để so sánh */
        // Dùng $lookup dạng pipeline vì cần $toString
        AggregationOperation lookupUsers = ctx -> new Document("$lookup",
                new Document("from", "users")
                        .append("let", Map.of("uid", "$_id")) // userId dạng String
                        .append("pipeline", List.of(
                                new Document("$match",
                                        new Document("$expr",
                                                new Document("$eq", List.of(
                                                        new Document("$toString", "$_id"), // chuyển ObjectId -> String
                                                        "$$uid")))),
                                new Document("$project",
                                        new Document("_id", 0)
                                                .append("fullName", 1)
                                                .append("email", 1)
                                                .append("phone", 1))))
                        .append("as", "user"));

        /* B4: UNWIND & PROJECT */
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

    @Override
    public TopAvgOrderValueUserDTO findUserWithHighestAvgOrderValue() {
        /*
         * 1️⃣ GROUP theo userId (String) và tính giá trị trung bình, tổng đơn, tổng chi
         */
        GroupOperation groupByUser = Aggregation.group("userId")
                .avg("totalAmount").as("avgOrderValue")
                .sum("totalAmount").as("totalSpent")
                .count().as("totalOrders");

        /* 2️⃣ SORT giảm dần theo avgOrderValue và LIMIT 1 */
        SortOperation sortDesc = Aggregation.sort(
                Sort.Direction.DESC, "avgOrderValue");
        LimitOperation limit1 = Aggregation.limit(1);

        /* 3️⃣ LOOKUP sang users bằng cách ép _id -> String rồi so sánh với userId */
        AggregationOperation lookupUser = ctx -> new Document("$lookup",
                new Document("from", "users")
                        .append("let", Map.of("uid", "$_id")) // _id ở đây là userId (string)
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

        /* 4️⃣ PROJECT thành định dạng DTO */
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

        /* Thực thi và map kết quả */
        AggregationResults<TopAvgOrderValueUserDTO> results = mongoTemplate.aggregate(pipeline, "orders",
                TopAvgOrderValueUserDTO.class);

        return results.getUniqueMappedResult(); // có thể trả null nếu không có đơn nào
    }
}
