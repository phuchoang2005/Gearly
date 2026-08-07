package com.dominator.gearly.ordering.api;

import com.dominator.gearly.shared.api.MessageResponse;
import com.dominator.gearly.ordering.application.CancelOrderCommand;
import com.dominator.gearly.ordering.application.CancelOrderService;
import com.dominator.gearly.ordering.application.OnlinePaymentService;
import com.dominator.gearly.ordering.application.OrderQueryService;
import com.dominator.gearly.ordering.application.PlaceOrderCommand;
import com.dominator.gearly.ordering.application.PlaceOrderService;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.platform.security.AuthenticatedUser;
import com.dominator.gearly.shared.domain.UserId;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * The customer-facing order endpoints.
 *
 * <p>This is where the authenticated principal stops. Every call below unwraps
 * {@link AuthenticatedUser} — a Spring Security {@code UserDetails} — into a {@link UserId}
 * before calling in, so no application service has a security type in its signature and every
 * one of them is constructible in a test with no security context.
 *
 * <p>URLs, request bodies and response shapes are unchanged.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final PlaceOrderService placeOrderService;
    private final CancelOrderService cancelOrderService;
    private final OrderQueryService orderQueryService;
    private final OnlinePaymentService onlinePaymentService;
    private final OrderResponseMapper orderResponseMapper;

    @GetMapping
    public ResponseEntity<Page<OrderResponseDTO>> getOrders(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(orderQueryService.search(callerId(authUser), search, status, page)
                .map(orderResponseMapper::toResponseDto));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getOrderStats(
            @AuthenticationPrincipal AuthenticatedUser authUser
    ) {
        return ResponseEntity.ok(orderQueryService.countByStatus(callerId(authUser)));
    }

    @PostMapping("/cancel-order")
    public ResponseEntity<MessageResponse> cancelOrder(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody CancelOrderRequestDTO dto
    ) {
        cancelOrderService.cancel(callerId(authUser),
                new CancelOrderCommand(dto.getOrderId(), dto.getReason()));
        return ResponseEntity.ok(new MessageResponse("Order cancelled successfully."));
    }

    /**
     * One of the caller's own orders. The principal is unwrapped and passed in like every
     * other call here — which is what closes the S12 IDOR: until this sprint this was the one
     * method on this controller that took an id and no caller.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDTO> getOrderById(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @PathVariable String orderId
    ) {
        return ResponseEntity.ok(orderResponseMapper.toResponseDto(
                orderQueryService.findById(callerId(authUser), orderId)));
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> createOrder(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody OrderCreationRequestDTO request
    ) {
        Order created = placeOrderService.place(callerId(authUser), toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponseMapper.toResponseDto(created));
    }

    @PostMapping("/momo")
    public ResponseEntity<CreateOrderResponse> createOrderWithMomo(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody OrderCreationRequestDTO request
    ) {
        OnlinePaymentService.Checkout checkout =
                onlinePaymentService.startCheckout(callerId(authUser), toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateOrderResponse(checkout.orderId(), checkout.payUrl()));
    }

    private UserId callerId(AuthenticatedUser authUser) {
        return authUser.id();
    }

    private PlaceOrderCommand toCommand(OrderCreationRequestDTO request) {
        return new PlaceOrderCommand(
                request.getItems().stream()
                        .map(item -> new PlaceOrderCommand.RequestedLine(
                                item.getProductId(), item.getQuantity()))
                        .toList(),
                request.getPaymentInfo().getMethod(),
                request.getShippingInformation());
    }
}
