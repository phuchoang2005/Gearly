package com.dominator.gearly.controller.admin;

import com.dominator.gearly.dto.OrderPatchDTO;
import com.dominator.gearly.dto.OrderUpsertRequestDTO;
import com.dominator.gearly.dto.QuantitySoldDTO;
import com.dominator.gearly.dto.TopSellerDTO;
import com.dominator.gearly.model.Order;
import com.dominator.gearly.model.OrderStatus;
import com.dominator.gearly.model.TimeFrame;
import com.dominator.gearly.service.admin.AdminOrderService;
import com.dominator.gearly.service.admin.OrderAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {
    private final AdminOrderService orderService;
    private final OrderAnalyticsService orderAnalyticsService;

    @GetMapping
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable String id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    @GetMapping("/quantity-sold")
    public ResponseEntity<List<QuantitySoldDTO>> getQuantitySoldByProduct(
            @RequestParam(defaultValue = "ALL") TimeFrame period
    ) {
        return ResponseEntity.ok(orderAnalyticsService.getQuantitySold(period));
    }

    @GetMapping("/top5")
    public ResponseEntity<List<TopSellerDTO>> getTop5BestSelling(
            @RequestParam(defaultValue = "ALL") TimeFrame period
    ) {
        return ResponseEntity.ok(orderAnalyticsService.getTop5BestSelling(period));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Order> updateOrder(@PathVariable String id, @RequestBody OrderUpsertRequestDTO newOrder) {
        return ResponseEntity.ok(orderService.updateOrder(id, newOrder));
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderUpsertRequestDTO newOrder) {
        return ResponseEntity.ok(orderService.createOrder(newOrder));
    }

    @PostMapping("/{id}/set-cancel")
    public ResponseEntity<Boolean> cancelOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.transition(id, OrderStatus.CANCELLED));
    }

    @PostMapping("/{id}/set-complete")
    public ResponseEntity<Boolean> completeOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.transition(id, OrderStatus.COMPLETED));
    }

    @PostMapping("/{id}/set-process")
    public ResponseEntity<Boolean> processOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.transition(id, OrderStatus.PROCESSING));
    }

    @PostMapping("/{id}/set-ship")
    public ResponseEntity<Boolean> setShipOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.transition(id, OrderStatus.SHIPPED));
    }

    @PostMapping("/{id}/set-delivered")
    public ResponseEntity<Boolean> setDeliveredOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.transition(id, OrderStatus.DELIVERED));
    }

    @PostMapping("/{id}/set-pending-refund")
    public ResponseEntity<Boolean> setPendingRefundOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.transition(id, OrderStatus.PENDING_REFUND));
    }

    @PostMapping("/{id}/set-refund")
    public ResponseEntity<Boolean> setRefundedOrder(@PathVariable String id) {
        return ResponseEntity.ok(orderService.transition(id, OrderStatus.REFUNDED));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Order> patchOrder(@PathVariable String id, @RequestBody OrderPatchDTO orderPatch) {
        return ResponseEntity.ok(orderService.patchOrder(id, orderPatch));
    }
}
