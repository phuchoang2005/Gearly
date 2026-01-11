package com.dominator.bookify.controller.admin;

import com.dominator.bookify.dto.*;

import java.util.List;

public interface AdminDashboardController {
    public List<TopCategoryQuantityDTO> getTopQuantityPerCategory();
    public List<BestSellerDTO> getTopBooks();
    public List<LoyalCustomerDTO>getTopLoyalCustomers();
    public TopAvgOrderValueUserDTO getTopAvgOrderValueUser();
    public List<BookInLowStockDTO> getTopBooksInLowStock(); // less than 10
}
