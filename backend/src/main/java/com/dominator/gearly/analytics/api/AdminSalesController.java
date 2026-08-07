package com.dominator.gearly.analytics.api;

import com.dominator.gearly.analytics.application.SalesAnalyticsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The two sales reports the admin console draws its charts from.
 *
 * <p>They were handlers on {@code AdminOrderController}, which meant {@code ordering.api} held a
 * field of the read side's query service and took a {@code TimeFrame} — a reporting concept —
 * in a signature. Reporting is not an ordering use case; it is the query side reading the same
 * documents ordering happens to write.
 *
 * <p><b>The URLs are unchanged.</b> This controller is mapped at {@code /api/admin/orders} on
 * purpose, so {@code /quantity-sold} and {@code /top5} answer exactly where they always have.
 * Spring is content with two controllers sharing a base path as long as no full path collides,
 * and none does. Splitting the class is an internal move; the admin console sees nothing.
 *
 * <p><b>Defence in depth.</b> {@code @PreAuthorize("hasRole('ADMIN')")} repeats the
 * {@code /api/admin/**} URL rule at the class level rather than replacing it, as every other
 * admin controller does.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSalesController {

    private final SalesAnalyticsQuery salesAnalytics;

    @GetMapping("/quantity-sold")
    public ResponseEntity<List<QuantitySoldDTO>> getQuantitySoldByProduct(
            @RequestParam(defaultValue = "ALL") TimeFrame period
    ) {
        return ResponseEntity.ok(salesAnalytics.getQuantitySold(period));
    }

    @GetMapping("/top5")
    public ResponseEntity<List<TopSellerDTO>> getTop5BestSelling(
            @RequestParam(defaultValue = "ALL") TimeFrame period
    ) {
        return ResponseEntity.ok(salesAnalytics.getTop5BestSelling(period));
    }
}
