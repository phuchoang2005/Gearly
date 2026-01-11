package com.dominator.bookify.service.admin;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
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
import com.dominator.bookify.repository.BooksInStockRepository;

import org.springframework.data.mongodb.core.aggregation.Aggregation;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDashboardGetBookImp implements AdminDashboardGetBookInterface {
    @Autowired
    private final BooksInStockRepository booksInStock;
    private final MongoTemplate mongoTemplate;

    @Override
    public List<BestSellerDTO> getTop10BestSellingBooks() {
        // 1) $unwind items
        AggregationOperation unwindItems = Aggregation.unwind("items");

        // 2) $group by items.bookId
        AggregationOperation groupByBook = Aggregation.group("items.bookId").sum("items.quantity").as("totalSold");

        // 3) $sort & $limit
        AggregationOperation sort = Aggregation.sort(Sort.Direction.DESC, "totalSold");
        AggregationOperation limit = Aggregation.limit(10);

        /* 4) $lookup (pipeline) – vì bookId (String) cần ép sang ObjectId để so sánh */
        Document lookupStage = new Document("$lookup", new Document("from", "books").append("let", Map.of("bookIdStr", "$_id")).append("pipeline", List.of(new Document("$match", new Document("$expr", new Document("$eq", List.of("$_id", new Document("$toObjectId", "$$bookIdStr"))))), new Document("$project", new Document("title", 1).append("authors", 1).append("price", 1)))).append("as", "book"));
        AggregationOperation lookup = context -> lookupStage;

        // 5) $unwind book & $project kết quả
        AggregationOperation unwindBook = Aggregation.unwind("book");
        AggregationOperation project = Aggregation.project().and("book._id").as("bookId").and("book.title").as("title").and("book.authors").as("authors").and("book.price").as("price").andInclude("totalSold");

        // Gộp pipeline
        Aggregation agg = Aggregation.newAggregation(unwindItems, groupByBook, sort, limit, lookup, unwindBook, project);

        return mongoTemplate.aggregate(agg, "orders", BestSellerDTO.class).getMappedResults();
    }

    @Override
    public List<TopCategoryQuantityDTO> getTop10BooksPerCategory() {
        // Giai đoạn 1: Tách các mặt hàng trong mỗi đơn hàng ra thành các document riêng
        // biệt
        UnwindOperation unwindItems = Aggregation.unwind("items");

        // Giai đoạn 2: Nhóm theo mã sách (bookId) để tính tổng số lượng đã bán của mỗi
        // cuốn sách
        GroupOperation groupBooks = Aggregation.group("items.bookId").sum("items.quantity").as("totalQuantitySold");

        // Giai đoạn 3: Chuyển đổi _id (hiện là chuỗi) sang ObjectId để có thể join với
        // collection 'books'
        AddFieldsOperation addConvertId = Aggregation.addFields().addField("bookObjectId").withValueOf(ConvertOperators.ToObjectId.toObjectId("$_id")).build();

        // Giai đoạn 4: Join với collection 'books' để lấy thông tin chi tiết của sách,
        // bao gồm cả categoryIds
        LookupOperation lookupBooks = Aggregation.lookup("books", "bookObjectId", "_id", "bookDetails");

        // Giai đoạn 5: Tách mảng bookDetails ra, vì mỗi sách chỉ có một chi tiết
        UnwindOperation unwindBookDetails = Aggregation.unwind("bookDetails");

        // Giai đoạn 6: Tách mảng categoryIds ra, vì một cuốn sách có thể thuộc nhiều
        // danh mục
        UnwindOperation unwindCategoryIds = Aggregation.unwind("bookDetails.categoryIds");

        // Giai đoạn 7: Join với collection 'categories' để lấy tên của danh mục
        LookupOperation lookupCategories = Aggregation.lookup("categories", "bookDetails.categoryIds", "_id", "categoryDetails");

        // Giai đoạn 8: Tách mảng categoryDetails
        UnwindOperation unwindCategoryDetails = Aggregation.unwind("categoryDetails");

        // Giai đoạn 9: Sắp xếp các sản phẩm theo số lượng bán giảm dần cho mỗi danh mục
        SortOperation sortOperation = Aggregation.sort(Sort.by(Sort.Direction.ASC, "categoryDetails.name")).and( // Vẫn sắp xếp theo tên để đảm
                // bảo
                // thứ tự tốt
                Sort.by(Sort.Direction.DESC, "totalQuantitySold"));

        // Giai đoạn 10: Nhóm các sách lại theo ID danh mục và đẩy thông tin sách vào
        // một mảng
        GroupOperation groupByCategory = Aggregation.group("bookDetails.categoryIds") // Thay đổi từ
                // categoryDetails.name
                // sang
                // bookDetails.categoryIds
                .first("categoryDetails.name").as("categoryName") // Lấy tên danh mục đầu tiên
                .push(new Document("bookId", "$_id").append("title", "$bookDetails.title").append("totalQuantitySold", "$totalQuantitySold")).as("books");

        // Giai đoạn 11: Định dạng lại output, lấy 10 sản phẩm đầu tiên trong mỗi mảng
        // 'books'
        ProjectionOperation projectOutput = Aggregation.project().andExclude("_id").and("$_id").as("categoryId") // Đổi tên _id thành categoryId cho DTO
                .and("categoryName").as("categoryName") // Giữ lại categoryName
                .and(ArrayOperators.Slice.sliceArrayOf("$books").offset(0).itemCount(10)).as("top10Books");

        // Tập hợp tất cả các giai đoạn aggregation
        Aggregation aggregation = Aggregation.newAggregation(unwindItems, groupBooks, addConvertId, lookupBooks, unwindBookDetails, unwindCategoryIds, lookupCategories, unwindCategoryDetails, sortOperation, groupByCategory, projectOutput);

        // Thực thi aggregation và ánh xạ kết quả vào DTO
        AggregationResults<TopCategoryQuantityDTO> results = mongoTemplate.aggregate(aggregation, "orders", TopCategoryQuantityDTO.class);

        return results.getMappedResults();
    }

    @Override
    public List<BookInLowStockDTO> getBookWithLowStock() {
        return booksInStock.findBooksWithLowStock().stream().map(book -> {
            BookInLowStockDTO bookWithLowStockDTO = new BookInLowStockDTO();
            bookWithLowStockDTO.setTitle(book.getTitle());
            bookWithLowStockDTO.setId(book.getId());
            bookWithLowStockDTO.setStock(book.getStock());
            return bookWithLowStockDTO;
        }).collect(Collectors.toList());
    }
}
