package com.dominator.bookify.service.admin;

import com.dominator.bookify.dto.*;

import java.util.List;

public interface AdminDashboardService {
    public List<BestSellerDTO> getTop10BestSellingBooks();

    public List<TopCategoryQuantityDTO> getTop10BooksPerCategory();

    public List<LoyalCustomerDTO> getTop10LoyalCustomers();

    public TopAvgOrderValueUserDTO findUserWithHighestAvgOrderValue();

    public List<BookInLowStockDTO> getBookWithLowStock();
}
