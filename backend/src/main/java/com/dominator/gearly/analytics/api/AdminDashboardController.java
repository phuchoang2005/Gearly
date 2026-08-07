package com.dominator.gearly.analytics.api;

import com.dominator.gearly.analytics.application.DashboardCustomerQuery;
import com.dominator.gearly.analytics.application.DashboardProductQuery;
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
 *
 * <h2>S13: the pass-through in the middle is gone</h2>
 * This called {@code AdminDashboardService}, thirty-eight lines whose every method was
 * {@code return getXImp.sameMethodName();}. A layer that forwards and does nothing else is a
 * file people have to open to discover it had nothing to say. The two query classes are
 * injected directly.
 *
 * <p>{@code /product-in-low-stock} kept its URL and moved to
 * {@code catalog.api.AdminLowStockController} — see {@code DashboardProductQuery} for why the
 * catalog answers that one.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardProductQuery products;
    private final DashboardCustomerQuery customers;

    @GetMapping("/top-by-category")
    public ResponseEntity<List<TopCategoryQuantityDTO>> getTopQuantityPerCategory() {
        return ResponseEntity.ok(products.getTop10ProductsPerCategory());
    }

    @GetMapping("/top-products")
    public ResponseEntity<List<BestSellerDTO>> getTopProducts() {
        return ResponseEntity.ok(products.getTop10BestSellingProducts());
    }

    @GetMapping("/loyal-customers")
    public ResponseEntity<List<LoyalCustomerDTO>> getTopLoyalCustomers() {
        return ResponseEntity.ok(customers.getTop10LoyalCustomers());
    }

    @GetMapping("/top-user-by-avg-order-value")
    public ResponseEntity<TopAvgOrderValueUserDTO> getTopAvgOrderValueUser() {
        return ResponseEntity.ok(customers.findUserWithHighestAvgOrderValue());
    }
}
