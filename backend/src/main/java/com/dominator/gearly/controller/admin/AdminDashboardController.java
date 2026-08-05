package com.dominator.gearly.controller.admin;

import com.dominator.gearly.catalog.api.ProductInLowStockDTO;
import com.dominator.gearly.dto.*;
import com.dominator.gearly.service.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The admin console's dashboard panels. Read-only aggregations over orders and products.
 *
 * <p><b>Defence in depth.</b> {@code @PreAuthorize("hasRole('ADMIN')")} repeats the
 * {@code /api/admin/**} URL rule from {@code SecurityConfig} at the class level rather than
 * replacing it, for the reason {@code AdminUserController} spells out: the URL rule is a prefix
 * match on a string, and every endpoint that has ever escaped one did so by being mounted
 * somewhere the pattern did not reach. The annotation travels with the code.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
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
