package com.dominator.bookify.service.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.dominator.bookify.dto.BestSellerDTO;
import com.dominator.bookify.dto.BookInLowStockDTO;
import com.dominator.bookify.dto.LoyalCustomerDTO;
import com.dominator.bookify.dto.TopAvgOrderValueUserDTO;
import com.dominator.bookify.dto.TopCategoryQuantityDTO;

import lombok.RequiredArgsConstructor;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImp implements AdminDashboardService {
        @Autowired
        private final AdminDashboardGetBookImp getBookImp;
        private final AdminDashboardGetUserImp getUserImp;

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