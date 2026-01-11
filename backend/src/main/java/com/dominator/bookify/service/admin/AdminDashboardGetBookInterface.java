package com.dominator.bookify.service.admin;

import java.util.List;

import com.dominator.bookify.dto.BestSellerDTO;
import com.dominator.bookify.dto.BookInLowStockDTO;
import com.dominator.bookify.dto.TopCategoryQuantityDTO;

public interface AdminDashboardGetBookInterface {
    public List<BestSellerDTO> getTop10BestSellingBooks();

    public List<TopCategoryQuantityDTO> getTop10BooksPerCategory();

    public List<BookInLowStockDTO> getBookWithLowStock();
}
