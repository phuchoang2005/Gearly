package com.dominator.bookify.controller.user;

import com.dominator.bookify.dto.CancelOrderRequestDTO;
import com.dominator.bookify.dto.CreateOrderResponse;
import com.dominator.bookify.dto.MessageResponse;
import com.dominator.bookify.dto.OrderCreationRequestDTO;
import com.dominator.bookify.model.Order;
import com.dominator.bookify.security.AuthenticatedUser;
import com.dominator.bookify.service.user.CustomerOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final CustomerOrderService orderService;

    @GetMapping
    public ResponseEntity<Page<Order>> getOrders(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(orderService.searchOrders(authUser, search, status, page));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getOrderStats(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(orderService.getOrderCountsByStatus(authUser));
    }

    @PostMapping("/cancel-order")
    public ResponseEntity<MessageResponse> cancelOrder(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody CancelOrderRequestDTO dto
    ) {
        orderService.cancelOrder(authUser, dto);
        return ResponseEntity.ok(new MessageResponse("Order cancelled successfully."));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable String orderId) {
        return ResponseEntity.ok(orderService.findById(orderId));
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody OrderCreationRequestDTO orderRequestDTO
    ) {
        Order createdOrder = orderService.createOrder(authUser, orderRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);
    }

    @PostMapping("/momo")
    public ResponseEntity<CreateOrderResponse> createOrderWithMomo(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody OrderCreationRequestDTO orderRequestDTO
    ) {
        CreateOrderResponse redirectRes = orderService.createOrderAndGetMomoUrl(authUser, orderRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(redirectRes);
    }
}
