package com.dominator.gearly.service.user;

import com.dominator.gearly.dto.CancelOrderRequestDTO;
import com.dominator.gearly.dto.CreateOrderResponse;
import com.dominator.gearly.dto.OrderCreationRequestDTO;
import com.dominator.gearly.dto.OrderItemRequestDTO;
import com.dominator.gearly.mapper.OrderMapper;
import com.dominator.gearly.model.*;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.ordering.domain.TransactionStatus;
import com.dominator.gearly.repository.OrderRepository;
import com.dominator.gearly.security.AuthenticatedUser;
import com.dominator.gearly.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dominator.gearly.exception.ApiException;
import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ConflictException;
import com.dominator.gearly.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomerOrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;
    private final CartService cartService;
    private final MomoService momoService;
    private final OrderMapper orderMapper;

    /**
     * This service, resolved through the Spring proxy. Needed because a plain
     * {@code this.createOrder(...)} call would bypass the proxy and with it the
     * {@code @Transactional} advice — see {@link #createOrderAndGetMomoUrl}. An
     * {@code ObjectProvider} rather than a direct self-injection because the latter is a
     * constructor cycle.
     *
     * <p>Temporary. S10 replaces this with an {@code OrderPlaced} event handled
     * {@code AFTER_COMMIT}, at which point the seam disappears.
     */
    private final ObjectProvider<CustomerOrderService> self;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final Money SHIPPING_COST_THRESHOLD = Money.of("30.00");
    private static final Money DEFAULT_SHIPPING_COST = Money.of("15.00");
    private static final Money FREE_SHIPPING_COST = Money.ZERO;

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

    @Transactional
    public void cancelOrder(AuthenticatedUser authUser, CancelOrderRequestDTO dto) {
        User user = authUser.getUser();

        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUserId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You are not allowed to cancel this order");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new ConflictException("This order already has status that cannot be cancelled");
        }

        Payment payment = order.getPayment();
        boolean alreadyPaid = payment.getTransactions().stream()
                .anyMatch(tx -> tx.getStatus() == TransactionStatus.SUCCESSFUL);

        if (alreadyPaid) {
            initiateRefund(order, payment);
            order.setOrderStatus(OrderStatus.PENDING_REFUND);
        } else {
            order.setOrderStatus(OrderStatus.CANCELLED);
        }

        order.setNote(dto.getReason());
        order.setModifiedAt(Instant.now());
        orderRepository.save(order);
    }

    public void initiateRefund(Order order, Payment payment) {
        Transaction refundTransaction = new Transaction();
        refundTransaction.setTransactionId(UUID.randomUUID().toString());
        refundTransaction.setStatus(TransactionStatus.PENDING_REFUND);
        refundTransaction.setAmount(order.getTotalAmount());
        refundTransaction.setRawResponse("Refund initiated for order: " + order.getId());
        refundTransaction.setCreatedAt(Instant.now());

        payment.getTransactions().add(refundTransaction);
    }

    @Transactional
    public Order createOrder(AuthenticatedUser authenticatedUser, OrderCreationRequestDTO requestDTO) {
        String userId = authenticatedUser.getUser().getId();
        List<OrderItem> orderItems = buildOrderItems(requestDTO.getItems());

        Money itemsSubtotal = itemsSubtotal(orderItems);
        Money shippingCost = calculateShippingCost(itemsSubtotal);
        Money taxes = itemsSubtotal.times(TAX_RATE);
        Money grandTotalUsd = itemsSubtotal.plus(taxes).plus(shippingCost);

        Order order = new Order();
        order.setUserId(userId);
        order.setItems(orderItems);
        order.setShippingInformation(requestDTO.getShippingInformation());
        order.setTotalAmount(grandTotalUsd);
        order.setPayment(buildInitialPayment(requestDTO.getPaymentInfo().getMethod(), grandTotalUsd));
        order.setOrderStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);

        applyStockAndClearCart(userId, orderItems);
        return savedOrder;
    }

    private List<OrderItem> buildOrderItems(List<OrderItemRequestDTO> itemRequests) {
        List<OrderItem> orderItems = new ArrayList<>();
        for (OrderItemRequestDTO itemRequest : itemRequests) {
            Product product = productService.getProductById(itemRequest.getProductId());
            int requestedQty = itemRequest.getQuantity();
            if (product.getStock() < requestedQty) {
                throw new BadRequestException("Insufficient stock for product: " + product.getTitle());
            }
            orderItems.add(orderMapper.toOrderItem(product, requestedQty));
        }
        return orderItems;
    }

    private Money itemsSubtotal(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(i -> i.getPrice().times(i.getQuantity()))
                .reduce(Money.ZERO, Money::plus);
    }

    private void applyStockAndClearCart(String userId, List<OrderItem> orderItems) {
        for (OrderItem item : orderItems) {
            productService.decreaseStock(item.getProductId(), item.getQuantity());
        }
        Map<String, Integer> qtyMap = orderItems.stream()
                .collect(Collectors.toMap(OrderItem::getProductId, OrderItem::getQuantity));
        cartService.removeItems(userId, null, qtyMap);
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
     * proxy, and the {@code @Transactional} on it would never be applied. That was the
     * behavior before this fix — with no transaction manager configured it made no
     * difference, but now that transactions are real it would have meant order placement
     * silently running unprotected.
     */
    public CreateOrderResponse createOrderAndGetMomoUrl(AuthenticatedUser authenticatedUser, OrderCreationRequestDTO requestDTO) {
        Order order = self.getObject().createOrder(authenticatedUser, requestDTO);
        // MoMo's client is a generic payment gateway and still speaks BigDecimal; S13 puts
        // it behind a PaymentGateway port that can take a Money directly.
        BigDecimal amountUsd = order.getTotalAmount().amount();
        String paymentUrl = momoService.createPaymentUrl(amountUsd, order.getId());
        return new CreateOrderResponse(order.getId(), paymentUrl);
    }

    private Money calculateShippingCost(Money subtotal) {
        return subtotal.isGreaterThan(SHIPPING_COST_THRESHOLD) ? FREE_SHIPPING_COST : DEFAULT_SHIPPING_COST;
    }

    private Payment buildInitialPayment(String method, Money amount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setAmount(amount);
        transaction.setRawResponse("Pending payment: " + amount.toDouble());
        transaction.setCreatedAt(Instant.now());

        Payment payment = new Payment();
        payment.setMethod(method);
        payment.setTransactions(List.of(transaction));
        return payment;
    }

    @Transactional
    public void updateOrderStatusFromMomo(String momoOrderId,
                                          String momoTransactionId,
                                          int resultCode,
                                          String rawResponse) {
        String ourOrderId = momoOrderId.replaceFirst("^Gearly-", "");
        Order order = orderRepository.findById(ourOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        Payment payment = order.getPayment();
        Transaction txn = new Transaction();
        txn.setTransactionId(momoTransactionId);
        txn.setStatus(resultCode == 0 ? TransactionStatus.SUCCESSFUL : TransactionStatus.FAILED);
        txn.setAmount(order.getTotalAmount());
        txn.setRawResponse(rawResponse);
        txn.setCreatedAt(Instant.now());

        payment.getTransactions().add(txn);

        if (resultCode == 0) {
            order.setOrderStatus(OrderStatus.PROCESSING);
        } else {
            order.setOrderStatus(OrderStatus.PENDING);
        }

        orderRepository.save(order);
    }
}
