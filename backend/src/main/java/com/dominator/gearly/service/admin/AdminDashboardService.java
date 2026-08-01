package com.dominator.gearly.service.admin;

import org.springframework.stereotype.Service;

import com.dominator.gearly.dto.BestSellerDTO;
import com.dominator.gearly.dto.BookInLowStockDTO;
import com.dominator.gearly.dto.LoyalCustomerDTO;
import com.dominator.gearly.dto.TopAvgOrderValueUserDTO;
import com.dominator.gearly.dto.TopCategoryQuantityDTO;

import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {
        private final AdminDashboardGetBookService getBookImp;
        private final AdminDashboardGetUserService getUserImp;

        public List<BestSellerDTO> getTop10BestSellingBooks() {
                return getBookImp.getTop10BestSellingBooks();
        }

        public List<TopCategoryQuantityDTO> getTop10BooksPerCategory() {
                return getBookImp.getTop10BooksPerCategory();
        }

        public List<LoyalCustomerDTO> getTop10LoyalCustomers() {
                return getUserImp.getTop10LoyalCustomers();
        }

        public TopAvgOrderValueUserDTO findUserWithHighestAvgOrderValue() {
                return getUserImp.findUserWithHighestAvgOrderValue();
        }

        public List<BookInLowStockDTO> getBookWithLowStock() {
                return getBookImp.getBookWithLowStock();
        }
}