package com.dominator.gearly.ordering.api;

import com.dominator.gearly.ordering.application.AdminOrderCommand;
import com.dominator.gearly.ordering.application.AdminOrderPatchCommand;
import com.dominator.gearly.ordering.application.AdminOrderService;
import com.dominator.gearly.ordering.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The admin order endpoints. URLs, request bodies and response shapes unchanged.
 *
 * <p>The seven {@code set-*} endpoints still answer {@code ResponseEntity<Boolean>}. An
 * illegal transition throws inside the aggregate now, but {@code AdminOrderService} catches it
 * and returns {@code false}, so the admin frontend needs no change. Every other write path —
 * {@code PUT}, {@code PATCH}, the customer cancel — lets it through to a 409.
 *
 * <p>S13 moved the two sales-report endpoints out. {@code /quantity-sold} and {@code /top5}
 * answer at the same URLs from {@code analytics.api.AdminSalesController}, which is mapped at
 * this same base path — reporting reads the documents ordering writes, but it is not an
 * ordering use case, and holding the read side's query service here made {@code ordering.api}
 * depend on a context downstream of it.
 *
 * <p><b>Defence in depth.</b> {@code @PreAuthorize("hasRole('ADMIN')")} repeats the
 * {@code /api/admin/**} URL rule from {@code SecurityConfig} at the class level rather than
 * replacing it, for the reason {@code AdminUserController} spells out: the URL rule is a prefix
 * match on a string, and every endpoint that has ever escaped one did so by being mounted
 * somewhere the pattern did not reach. The annotation travels with the code.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {

    private final AdminOrderService orderService;
    private final OrderResponseMapper orderResponseMapper;

    @GetMapping
    public ResponseEntity<List<OrderResponseDTO>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders().stream()
                .map(orderResponseMapper::toResponseDto).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable String id) {
        return ResponseEntity.ok(orderResponseMapper.toResponseDto(orderService.getOrderById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderResponseDTO> updateOrder(@PathVariable String id,
                                                        @RequestBody OrderUpsertRequestDTO newOrder) {
        return ResponseEntity.ok(orderResponseMapper.toResponseDto(
                orderService.replaceOrder(id, toCommand(newOrder))));
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(@RequestBody OrderUpsertRequestDTO newOrder) {
        return ResponseEntity.ok(orderResponseMapper.toResponseDto(
                orderService.createOrder(toCommand(newOrder))));
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
    public ResponseEntity<OrderResponseDTO> patchOrder(@PathVariable String id,
                                                       @RequestBody OrderPatchDTO orderPatch) {
        return ResponseEntity.ok(orderResponseMapper.toResponseDto(
                orderService.patchOrder(id, toCommand(orderPatch))));
    }

    /** Note what is <em>not</em> copied: {@code totalAmount}. See {@link AdminOrderCommand}. */
    private AdminOrderCommand toCommand(OrderUpsertRequestDTO dto) {
        return new AdminOrderCommand(
                dto.getUserId(),
                dto.getItems(),
                dto.getShippingInformation(),
                dto.getPayment(),
                dto.getOrderStatus(),
                dto.isReviewed(),
                dto.getNote(),
                dto.getDoneAt());
    }

    private AdminOrderPatchCommand toCommand(OrderPatchDTO dto) {
        return new AdminOrderPatchCommand(
                dto.getItems(),
                dto.getShippingInformation(),
                dto.getPayment(),
                dto.getOrderStatus(),
                dto.getDoneAt());
    }
}
