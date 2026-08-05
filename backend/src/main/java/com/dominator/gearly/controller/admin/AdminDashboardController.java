package com.dominator.gearly.controller.admin;

import com.dominator.gearly.catalog.api.ProductInLowStockDTO;
import com.dominator.gearly.dto.*;
import com.dominator.gearly.service.admin.AdminDashboardService;
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
        return ResponseEntity.ok(dashboardService.getTop10ProductsPerCategory());
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<BestSellerDTO>> getTopProducts() {
        return ResponseEntity.ok(dashboardService.getTop10BestSellingProducts());
    }

    @GetMapping("/loyal-customers")
    public ResponseEntity<List<LoyalCustomerDTO>> getTopLoyalCustomers() {
        return ResponseEntity.ok(dashboardService.getTop10LoyalCustomers());
    }

    @GetMapping("/top-user-by-avg-order-value")
    public ResponseEntity<TopAvgOrderValueUserDTO> getTopAvgOrderValueUser() {
        return ResponseEntity.ok(dashboardService.findUserWithHighestAvgOrderValue());
    }

    // products with stock less than 10
    @GetMapping("/product-in-low-stock")
    public ResponseEntity<List<ProductInLowStockDTO>> getTopProductsInLowStock() {
        return ResponseEntity.ok(dashboardService.getProductWithLowStock());
    }
}
