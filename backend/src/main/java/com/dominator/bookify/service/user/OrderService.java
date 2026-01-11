package com.dominator.bookify.service.user;

import com.dominator.bookify.dto.CancelOrderRequestDTO;
import com.dominator.bookify.dto.CreateOrderResponse;
import com.dominator.bookify.dto.OrderCreationRequestDTO;
import com.dominator.bookify.dto.OrderItemRequestDTO;
import com.dominator.bookify.model.*;
import com.dominator.bookify.repository.OrderRepository;
import com.dominator.bookify.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final BookService bookService;
    private final CartService cartService;
    private final MomoService momoService;

    private static final BigDecimal TAX_RATE = new BigDecimal("0.08");
    private static final BigDecimal SHIPPING_COST_THRESHOLD = new BigDecimal("30.00");
    private static final BigDecimal DEFAULT_SHIPPING_COST = new BigDecimal("15.00");
    private static final BigDecimal FREE_SHIPPING_COST = new BigDecimal("0.00");

    public Order findById(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getUserId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to cancel this order");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING && order.getOrderStatus() != OrderStatus.PROCESSING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This order already has status that cannot be cancelled");
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
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal itemsSubtotal = BigDecimal.ZERO;

        for (OrderItemRequestDTO itemRequest : requestDTO.getItems()) {
            Book book = bookService.getBookById(itemRequest.getBookId());
            int requestedQty = itemRequest.getQuantity();
            if (book.getStock() < requestedQty) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient stock for book: " + book.getTitle());
            }
            OrderItem orderItem = buildOrderItem(book, requestedQty);
            orderItems.add(orderItem);
            BigDecimal itemTotal = BigDecimal.valueOf(book.getPrice()).multiply(BigDecimal.valueOf(requestedQty));
            itemsSubtotal = itemsSubtotal.add(itemTotal);
        }

        BigDecimal shippingCost = calculateShippingCost(itemsSubtotal);
        BigDecimal taxes = itemsSubtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotalUsd = itemsSubtotal.add(taxes).add(shippingCost).setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.setUserId(userId);
        order.setItems(orderItems);
        order.setShippingInformation(requestDTO.getShippingInformation());
        order.setTotalAmount(grandTotalUsd.doubleValue());
        order.setPayment(buildInitialPayment(requestDTO.getPaymentInfo().getMethod(), grandTotalUsd));
        order.setOrderStatus(OrderStatus.PENDING);
        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : orderItems) {
            bookService.decreaseStock(item.getBookId(), item.getQuantity());
        }

        Map<String, Integer> qtyMap = orderItems.stream()
                .collect(Collectors.toMap(OrderItem::getBookId, OrderItem::getQuantity));
        cartService.removeItems(userId, null, qtyMap);

        return savedOrder;
    }

    @Transactional
    public CreateOrderResponse createOrderAndGetMomoUrl(AuthenticatedUser authenticatedUser, OrderCreationRequestDTO requestDTO) {
        Order order = createOrder(authenticatedUser, requestDTO);
        BigDecimal amountUsd = BigDecimal.valueOf(order.getTotalAmount());
        String paymentUrl = momoService.createPaymentUrl(amountUsd, order.getId());
        return new CreateOrderResponse(order.getId(), paymentUrl);
    }

    private OrderItem buildOrderItem(Book book, int quantity) {
        OrderItem item = new OrderItem();
        item.setBookId(book.getId());
        item.setImageUrl(book.getImages().getFirst().getUrl());
        item.setTitle(book.getTitle());
        item.setPrice(book.getPrice());
        item.setQuantity(quantity);
        return item;
    }

    private BigDecimal calculateShippingCost(BigDecimal subtotal) {
        return subtotal.compareTo(SHIPPING_COST_THRESHOLD) > 0 ? FREE_SHIPPING_COST : DEFAULT_SHIPPING_COST;
    }

    private Payment buildInitialPayment(String method, BigDecimal amount) {
        Transaction transaction = new Transaction();
        transaction.setTransactionId(UUID.randomUUID().toString());
        transaction.setStatus(TransactionStatus.PENDING);
        transaction.setAmount(amount.doubleValue());
        transaction.setRawResponse("Pending payment: " + amount.doubleValue());
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
        String ourOrderId = momoOrderId.replaceFirst("^Bookify-", "");
        Order order = orderRepository.findById(ourOrderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

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
