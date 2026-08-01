package com.dominator.gearly.service.admin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AddFieldsOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.data.mongodb.core.aggregation.ConvertOperators;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.ProjectionOperation;
import org.springframework.data.mongodb.core.aggregation.SortOperation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.stereotype.Service;

import com.dominator.gearly.dto.BestSellerDTO;
import com.dominator.gearly.dto.ProductInLowStockDTO;
import com.dominator.gearly.dto.TopCategoryQuantityDTO;
import com.dominator.gearly.mapper.ProductMapper;
import com.dominator.gearly.repository.ProductsInStockRepository;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardGetProductService {
    private final ProductsInStockRepository productsInStock;
    private final MongoTemplate mongoTemplate;
    private final ProductMapper productMapper;

    public List<BestSellerDTO> getTop10BestSellingProducts() {
        // 1) $unwind items
        AggregationOperation unwindItems = Aggregation.unwind("items");

        // 2) $group by items.productId
        AggregationOperation groupByProduct = Aggregation.group("items.productId").sum("items.quantity").as("totalSold");

        // 3) $sort & $limit
        AggregationOperation sort = Aggregation.sort(Sort.Direction.DESC, "totalSold");
        AggregationOperation limit = Aggregation.limit(10);

        /* 4) $lookup (pipeline) - productId (String) must be cast to ObjectId to compare */
        Document lookupStage = new Document("$lookup", new Document("from", "products").append("let", Map.of("productIdStr", "$_id")).append("pipeline", List.of(new Document("$match", new Document("$expr", new Document("$eq", List.of("$_id", new Document("$toObjectId", "$$productIdStr"))))), new Document("$project", new Document("title", 1).append("authors", 1).append("price", 1)))).append("as", "product"));
        AggregationOperation lookup = context -> lookupStage;

        // 5) $unwind product & $project the result
        AggregationOperation unwindProduct = Aggregation.unwind("product");
        AggregationOperation project = Aggregation.project().and("product._id").as("productId").and("product.title").as("title").and("product.authors").as("authors").and("product.price").as("price").andInclude("totalSold");

        // Assemble the pipeline
        Aggregation agg = Aggregation.newAggregation(unwindItems, groupByProduct, sort, limit, lookup, unwindProduct, project);

        return mongoTemplate.aggregate(agg, "orders", BestSellerDTO.class).getMappedResults();
    }

    public List<TopCategoryQuantityDTO> getTop10ProductsPerCategory() {
        // Stage 1: Unwind each order's items into separate documents
        UnwindOperation unwindItems = Aggregation.unwind("items");

        // Stage 2: Group by productId to sum the total quantity sold per product
        GroupOperation groupProducts = Aggregation.group("items.productId").sum("items.quantity").as("totalQuantitySold");

        // Stage 3: Convert _id (currently a string) to ObjectId so it can join the 'products' collection
        AddFieldsOperation addConvertId = Aggregation.addFields().addField("productObjectId").withValueOf(ConvertOperators.ToObjectId.toObjectId("$_id")).build();

        // Stage 4: Join the 'products' collection to fetch product details, including categoryIds
        LookupOperation lookupProducts = Aggregation.lookup("products", "productObjectId", "_id", "productDetails");

        // Stage 5: Unwind the productDetails array, since each product has exactly one detail
        UnwindOperation unwindProductDetails = Aggregation.unwind("productDetails");

        // Stage 6: Unwind the categoryIds array, since a product can belong to multiple categories
        UnwindOperation unwindCategoryIds = Aggregation.unwind("productDetails.categoryIds");

        // Stage 7: Join the 'categories' collection to fetch category names
        LookupOperation lookupCategories = Aggregation.lookup("categories", "productDetails.categoryIds", "_id", "categoryDetails");

        // Stage 8: Unwind the categoryDetails array
        UnwindOperation unwindCategoryDetails = Aggregation.unwind("categoryDetails");

        // Stage 9: Sort products by quantity sold (desc) within each category
        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.ASC, "categoryDetails.name")).and( // still sort by name
                // to keep a
                // stable order
                Sort.by(Sort.Direction.DESC, "totalQuantitySold"));

        // Stage 10: Group products by category id and push product info into an array
        GroupOperation groupByCategory = Aggregation.group("productDetails.categoryIds") // changed from
                // categoryDetails.name
                // to
                // productDetails.categoryIds
                .first("categoryDetails.name").as("categoryName") // take the first category name
                .push(new Document("productId", "$_id").append("title", "$productDetails.title").append("totalQuantitySold", "$totalQuantitySold")).as("products");

        // Stage 11: Reshape the output, taking the first 10 products in each 'products' array
        ProjectionOperation projectOutput = Aggregation.project().andExclude("_id").and("$_id").as("categoryId") // rename _id to categoryId for the DTO
                .and("categoryName").as("categoryName") // keep categoryName
                .and(ArrayOperators.Slice.sliceArrayOf("$products").offset(0).itemCount(10)).as("top10Products");

        // Assemble all aggregation stages
        Aggregation aggregation = Aggregation.newAggregation(unwindItems, groupProducts, addConvertId, lookupProducts, unwindProductDetails, unwindCategoryIds, lookupCategories, unwindCategoryDetails, sortOperation, groupByCategory, projectOutput);

        // Execute the aggregation and map results into the DTO
        AggregationResults<TopCategoryQuantityDTO> results = mongoTemplate.aggregate(aggregation, "orders", TopCategoryQuantityDTO.class);

        return results.getMappedResults();
    }

    public List<ProductInLowStockDTO> getProductWithLowStock() {
        return productsInStock.findProductsWithLowStock().stream()
                .map(productMapper::toLowStockDto)
                .collect(Collectors.toList());
    }
}
