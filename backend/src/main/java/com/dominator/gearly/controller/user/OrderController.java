package com.dominator.gearly.controller.user;

import com.dominator.gearly.dto.CancelOrderRequestDTO;
import com.dominator.gearly.dto.CreateOrderResponse;
import com.dominator.gearly.dto.MessageResponse;
import com.dominator.gearly.dto.OrderCreationRequestDTO;
import com.dominator.gearly.dto.OrderResponseDTO;
import com.dominator.gearly.mapper.OrderMapper;
import com.dominator.gearly.model.Order;
import com.dominator.gearly.security.AuthenticatedUser;
import com.dominator.gearly.service.user.CustomerOrderService;
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
    private final OrderMapper orderMapper;

    @GetMapping
    public ResponseEntity<Page<OrderResponseDTO>> getOrders(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(orderService.searchOrders(authUser, search, status, page)
                .map(orderMapper::toResponseDto));
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
    public ResponseEntity<OrderResponseDTO> getOrderById(@PathVariable String orderId) {
        return ResponseEntity.ok(orderMapper.toResponseDto(orderService.findById(orderId)));
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody OrderCreationRequestDTO orderRequestDTO
    ) {
        Order createdOrder = orderService.createOrder(authUser, orderRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toResponseDto(createdOrder));
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
