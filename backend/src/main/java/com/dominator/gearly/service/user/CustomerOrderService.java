package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.CancelOrderRequestDTO;
import com.dominator.gearly.dto.CreateOrderResponse;
import com.dominator.gearly.dto.OrderCreationRequestDTO;
import com.dominator.gearly.dto.OrderItemRequestDTO;
import com.dominator.gearly.exception.ApiException;
import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.mapper.OrderMapper;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.PricingPolicy;
import com.dominator.gearly.repository.OrderRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CartService cartService;
    private final MomoService momoService;
    private final OrderMapper orderMapper;
    private final PricingPolicy pricingPolicy;

    /**
     * This service, resolved through the Spring proxy. Needed because a plain
     * {@code this.createOrder(...)} call would bypass the proxy and with it the
     * {@code @Transactional} advice — see {@link #createOrderAndGetMomoUrl}. An
     * {@code ObjectProvider} rather than a direct self-injection because the latter is a
     * constructor cycle.
     *
     * <p>Temporary. Goes away when placement moves into {@code ordering.application}, where
     * the transactional half and the gateway half are separate beans and there is no
     * self-call left to route.
     */
    private final ObjectProvider<CustomerOrderService> self;

    public Order findById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    public Page<Order> searchOrders(AuthenticatedUser authUser,
                                    String searchTerm,
                                    String status,
                                    int page) {
        String userId = authUser.getUser().getId();
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "modifiedAt"));

        boolean hasSearch = (searchTerm != null && !searchTerm.trim().isEmpty());
        boolean hasStatus = (status != null && !status.trim().isEmpty());

        if (!hasSearch && !hasStatus) {
            return orderRepository.findByUserId(userId, pageable);
        }

        if (!hasSearch) {
            return orderRepository.findByUserIdAndOrderStatus(userId, OrderStatus.valueOf(status.trim()), pageable);
        }

        return orderRepository.searchOrders(userId, status, searchTerm.trim(), pageable);
    }

    public Map<String, Long> getOrderCountsByStatus(AuthenticatedUser authUser) {
        String userId = authUser.getUser().getId();

        // Count orders by each individual status
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            long count = orderRepository.countByUserIdAndOrderStatus(userId, status);
            statusCounts.put(status.name(), count);
        }

        // Count “in‐progress” orders
        List<OrderStatus> finalStatuses = List.of(
                OrderStatus.COMPLETED,
                OrderStatus.CANCELLED,
                OrderStatus.REFUNDED
        );
        long inProgress = orderRepository.countByUserIdAndOrderStatusNotIn(userId, finalStatuses);
        statusCounts.put("totalInProgress", inProgress);

        return statusCounts;
    }

    /**
     * The customer cancels. Ownership is checked here — it decides a 403, which is a web
     * concern — and everything else is the aggregate's: whether the order has gone too far to
     * cancel, whether the money has arrived and a refund is therefore owed, and which status
     * that lands on.
     */
    @Transactional
    public void cancelOrder(AuthenticatedUser authUser, CancelOrderRequestDTO dto) {
        UserId userId = UserId.of(authUser.getUser().getId());

        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.isOwnedBy(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to cancel this order");
        }

        order.cancel(dto.getReason());
        orderRepository.save(order);
    }

    @Transactional
    public Order createOrder(AuthenticatedUser authenticatedUser, OrderCreationRequestDTO requestDTO) {
        UserId userId = UserId.of(authenticatedUser.getUser().getId());
        List<OrderLine> orderLines = buildOrderLines(requestDTO.getItems());

        Order order = Order.place(
                userId,
                orderLines,
                requestDTO.getShippingInformation(),
                requestDTO.getPaymentInfo().getMethod(),
                pricingPolicy);
        Order savedOrder = orderRepository.save(order);

        applyStockAndClearCart(userId, orderLines);
        return savedOrder;
    }

    private List<OrderLine> buildOrderLines(List<OrderItemRequestDTO> itemRequests) {
        List<OrderLine> orderLines = new ArrayList<>();
        for (OrderItemRequestDTO itemRequest : itemRequests) {
            Product product = productService.getProductById(itemRequest.getProductId());
            int requestedQty = itemRequest.getQuantity();
            if (product.getStock() < requestedQty) {
                throw new BadRequestException("Insufficient stock for product: " + product.getTitle());
            }
            orderLines.add(orderMapper.toOrderLine(product, requestedQty));
        }
        return orderLines;
    }

    private void applyStockAndClearCart(UserId userId, List<OrderLine> orderLines) {
        for (OrderLine line : orderLines) {
            productService.decreaseStock(line.getProductId().value(), line.getQuantity().toInt());
        }
        Map<String, Integer> qtyMap = orderLines.stream()
                .collect(Collectors.toMap(
                        line -> line.getProductId().value(),
                        line -> line.getQuantity().toInt()));
        cartService.removeItems(userId.value(), null, qtyMap);
    }

    /**
     * Place an order and hand back the gateway URL the customer is redirected to.
     *
     * <p>Deliberately <b>not</b> {@code @Transactional}. The transaction belongs to
     * {@link #createOrder} alone — one aggregate, one transaction — and it must be closed
     * before the MoMo call goes out, so a slow or unreachable third party can never hold a
     * database transaction open.
     *
     * <p>The call therefore has to go through {@link #self}: invoking
     * {@code createOrder(...)} directly would resolve on {@code this} rather than on the
     * proxy, and the {@code @Transactional} on it would never be applied.
     */
    public CreateOrderResponse createOrderAndGetMomoUrl(AuthenticatedUser authenticatedUser, OrderCreationRequestDTO requestDTO) {
        Order order = self.getObject().createOrder(authenticatedUser, requestDTO);
        // MoMo's client is a generic payment gateway and still speaks BigDecimal; S13 puts
        // it behind a PaymentGateway port that can take a Money directly.
        BigDecimal amountUsd = order.getTotalAmount().amount();
        String paymentUrl = momoService.createPaymentUrl(amountUsd, order.getId());
        return new CreateOrderResponse(order.getId(), paymentUrl);
    }

    /**
     * The gateway's IPN callback. Recording the transaction and moving the status are one
     * operation on the aggregate now, so a callback can no longer set a status the transition
     * table would refuse — a failed checkout returning the order to {@code PENDING} is a
     * declared edge in that table rather than an assignment nobody checked.
     */
    @Transactional
    public void updateOrderStatusFromMomo(String momoOrderId,
                                          String momoTransactionId,
                                          int resultCode,
                                          String rawResponse) {
        String ourOrderId = momoOrderId.replaceFirst("^Gearly-", "");
        Order order = orderRepository.findById(ourOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.recordGatewayResult(momoTransactionId, resultCode == 0, rawResponse);

        orderRepository.save(order);
    }
}
