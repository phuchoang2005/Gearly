package com.dominator.bookify.service.admin;

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

import com.dominator.bookify.dto.BestSellerDTO;
import com.dominator.bookify.dto.BookInLowStockDTO;
import com.dominator.bookify.dto.TopCategoryQuantityDTO;
import com.dominator.bookify.mapper.BookMapper;
import com.dominator.bookify.repository.BooksInStockRepository;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardGetBookService {
    private final BooksInStockRepository booksInStock;
    private final MongoTemplate mongoTemplate;
    private final BookMapper bookMapper;

    public List<BestSellerDTO> getTop10BestSellingBooks() {
        // 1) $unwind items
        AggregationOperation unwindItems = Aggregation.unwind("items");

        // 2) $group by items.bookId
        AggregationOperation groupByBook = Aggregation.group("items.bookId").sum("items.quantity").as("totalSold");

        // 3) $sort & $limit
        AggregationOperation sort = Aggregation.sort(Sort.Direction.DESC, "totalSold");
        AggregationOperation limit = Aggregation.limit(10);

        /* 4) $lookup (pipeline) - bookId (String) must be cast to ObjectId to compare */
        Document lookupStage = new Document("$lookup", new Document("from", "books").append("let", Map.of("bookIdStr", "$_id")).append("pipeline", List.of(new Document("$match", new Document("$expr", new Document("$eq", List.of("$_id", new Document("$toObjectId", "$$bookIdStr"))))), new Document("$project", new Document("title", 1).append("authors", 1).append("price", 1)))).append("as", "book"));
        AggregationOperation lookup = context -> lookupStage;

        // 5) $unwind book & $project the result
        AggregationOperation unwindBook = Aggregation.unwind("book");
        AggregationOperation project = Aggregation.project().and("book._id").as("bookId").and("book.title").as("title").and("book.authors").as("authors").and("book.price").as("price").andInclude("totalSold");

        // Assemble the pipeline
        Aggregation agg = Aggregation.newAggregation(unwindItems, groupByBook, sort, limit, lookup, unwindBook, project);

        return mongoTemplate.aggregate(agg, "orders", BestSellerDTO.class).getMappedResults();
    }

    public List<TopCategoryQuantityDTO> getTop10BooksPerCategory() {
        // Stage 1: Unwind each order's items into separate documents
        UnwindOperation unwindItems = Aggregation.unwind("items");

        // Stage 2: Group by bookId to sum the total quantity sold per book
        GroupOperation groupBooks = Aggregation.group("items.bookId").sum("items.quantity").as("totalQuantitySold");

        // Stage 3: Convert _id (currently a string) to ObjectId so it can join the 'books' collection
        AddFieldsOperation addConvertId = Aggregation.addFields().addField("bookObjectId").withValueOf(ConvertOperators.ToObjectId.toObjectId("$_id")).build();

        // Stage 4: Join the 'books' collection to fetch book details, including categoryIds
        LookupOperation lookupBooks = Aggregation.lookup("books", "bookObjectId", "_id", "bookDetails");

        // Stage 5: Unwind the bookDetails array, since each book has exactly one detail
        UnwindOperation unwindBookDetails = Aggregation.unwind("bookDetails");

        // Stage 6: Unwind the categoryIds array, since a book can belong to multiple categories
        UnwindOperation unwindCategoryIds = Aggregation.unwind("bookDetails.categoryIds");

        // Stage 7: Join the 'categories' collection to fetch category names
        LookupOperation lookupCategories = Aggregation.lookup("categories", "bookDetails.categoryIds", "_id", "categoryDetails");

        // Stage 8: Unwind the categoryDetails array
        UnwindOperation unwindCategoryDetails = Aggregation.unwind("categoryDetails");

        // Stage 9: Sort products by quantity sold (desc) within each category
        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.ASC, "categoryDetails.name")).and( // still sort by name
                // to keep a
                // stable order
                Sort.by(Sort.Direction.DESC, "totalQuantitySold"));

        // Stage 10: Group books by category id and push book info into an array
        GroupOperation groupByCategory = Aggregation.group("bookDetails.categoryIds") // changed from
                // categoryDetails.name
                // to
                // bookDetails.categoryIds
                .first("categoryDetails.name").as("categoryName") // take the first category name
                .push(new Document("bookId", "$_id").append("title", "$bookDetails.title").append("totalQuantitySold", "$totalQuantitySold")).as("books");

        // Stage 11: Reshape the output, taking the first 10 products in each 'books' array
        ProjectionOperation projectOutput = Aggregation.project().andExclude("_id").and("$_id").as("categoryId") // rename _id to categoryId for the DTO
                .and("categoryName").as("categoryName") // keep categoryName
                .and(ArrayOperators.Slice.sliceArrayOf("$books").offset(0).itemCount(10)).as("top10Books");

        // Assemble all aggregation stages
        Aggregation aggregation = Aggregation.newAggregation(unwindItems, groupBooks, addConvertId, lookupBooks, unwindBookDetails, unwindCategoryIds, lookupCategories, unwindCategoryDetails, sortOperation, groupByCategory, projectOutput);

        // Execute the aggregation and map results into the DTO
        AggregationResults<TopCategoryQuantityDTO> results = mongoTemplate.aggregate(aggregation, "orders", TopCategoryQuantityDTO.class);

        return results.getMappedResults();
    }

    public List<BookInLowStockDTO> getBookWithLowStock() {
        return booksInStock.findBooksWithLowStock().stream()
                .map(bookMapper::toLowStockDto)
                .collect(Collectors.toList());
    }
}
