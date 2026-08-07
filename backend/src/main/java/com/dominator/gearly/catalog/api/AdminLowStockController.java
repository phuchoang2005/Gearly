package com.dominator.gearly.catalog.api;

import com.dominator.gearly.catalog.application.ProductQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The dashboard's low-stock panel — a catalog question, served at a dashboard URL.
 *
 * <p>It used to arrive through {@code AdminDashboardController} →
 * {@code AdminDashboardService} → {@code AdminDashboardGetProductService} →
 * {@code catalog.application.ProductQueryService}: three hops to reach the context that could
 * answer it in the first place, the last of which was the read side reaching into another
 * context's application layer.
 *
 * <p>"Which products are running low" belongs here. The catalog owns stock, and since S11 it
 * owns the threshold that defines *low* — {@code gearly.catalog.low-stock-threshold}, which
 * replaced a hard-coded {@code $lt: 10} in a repository annotation. Nothing about the question
 * is analytics'; it only appeared there because the panel that shows it is on the dashboard.
 *
 * <p><b>The URL is unchanged.</b> It is declared in full on the method rather than through a
 * class-level {@code /api/admin/dashboard} mapping, because this controller owns exactly one
 * dashboard path and claiming the prefix would suggest otherwise.
 *
 * <p><b>Defence in depth.</b> {@code @PreAuthorize("hasRole('ADMIN')")} repeats the
 * {@code /api/admin/**} URL rule rather than replacing it, as every other admin controller does.
 */
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminLowStockController {

    private final ProductQueryService productQueryService;

    @GetMapping("/api/admin/dashboard/product-in-low-stock")
    public ResponseEntity<List<ProductInLowStockDTO>> getProductsInLowStock() {
        return ResponseEntity.ok(productQueryService.getLowStockProducts());
    }
}
