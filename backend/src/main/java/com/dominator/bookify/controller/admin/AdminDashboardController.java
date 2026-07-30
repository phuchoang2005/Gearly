package com.dominator.bookify.controller.admin;

import com.dominator.bookify.dto.*;
import com.dominator.bookify.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    @GetMapping("/top-by-category")
    public ResponseEntity<List<TopCategoryQuantityDTO>> getTopQuantityPerCategory() {
        return ResponseEntity.ok(dashboardService.getTop10BooksPerCategory());
    }

    @GetMapping("/top-books")
    public ResponseEntity<List<BestSellerDTO>> getTopBooks() {
        return ResponseEntity.ok(dashboardService.getTop10BestSellingBooks());
    }

    @GetMapping("/loyal-customers")
    public ResponseEntity<List<LoyalCustomerDTO>> getTopLoyalCustomers() {
        return ResponseEntity.ok(dashboardService.getTop10LoyalCustomers());
    }

    @GetMapping("/top-user-by-avg-order-value")
    public ResponseEntity<TopAvgOrderValueUserDTO> getTopAvgOrderValueUser() {
        return ResponseEntity.ok(dashboardService.findUserWithHighestAvgOrderValue());
    }

    // books with stock less than 10
    @GetMapping("/book-in-low-stock")
    public ResponseEntity<List<BookInLowStockDTO>> getTopBooksInLowStock() {
        return ResponseEntity.ok(dashboardService.getBookWithLowStock());
    }
}
