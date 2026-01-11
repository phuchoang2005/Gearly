package com.dominator.bookify.service.admin;

import com.dominator.bookify.dto.OrderPatchDTO;
import com.dominator.bookify.dto.QuantitySoldDTO;
import com.dominator.bookify.dto.TopSellerDTO;
import com.dominator.bookify.model.*;
import com.dominator.bookify.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;


@AllArgsConstructor
@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Order getOrderById(String id) {
        return orderRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    @Override
    public List<QuantitySoldDTO> getQuantitySold(TimeFrame timeFrame) {
        // Build match on status and timeframe
        MatchOperation match = buildMatch(timeFrame);

        // unwind items, group by bookId
        GroupOperation group = group("items.bookId")
                .first("items.title")
                .as("title")
                .sum("items.quantity")
                .as("totalSold");


        ProjectionOperation project = Aggregation.project()
                .and("_id").as("bookId")
                .and("title").as("title")
                .and("totalSold").as("totalSold")
                .andExclude("_id");

        Aggregation agg = newAggregation(
                match,
                unwind("items"),
                group,
                project,
                sort(Sort.by(Sort.Direction.DESC, "totalSold"))
        );
        return mongoTemplate
                .aggregate(agg, Order.class, QuantitySoldDTO.class)
                .getMappedResults();
    }

    @Override
    public List<TopSellerDTO> getTop5BestSelling(TimeFrame timeFrame) {
        MatchOperation match = buildMatch(timeFrame);

        UnwindOperation unwind = unwind("items");

        GroupOperation group = group("items.bookId")
                .sum("items.quantity").as("totalSold")
                .first("items.title").as("title");


        SortOperation sort = sort(Sort.by(Sort.Direction.DESC, "totalSold"));
        LimitOperation limit = limit(5);

        ProjectionOperation project = project()
                .and("_id").as("bookId")
                .and("totalSold").as("totalSold")
                .and("title").as("title")
                .andExclude("_id");

        Aggregation agg = newAggregation(
                match,
                unwind,
                group,
                sort,
                limit,
                project
        );

        return mongoTemplate
                .aggregate(agg, Order.class, TopSellerDTO.class)
                .getMappedResults();
    }

    @Override
    public Order updateOrder(String id, Order order) {
        Order exitstingOrder = orderRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        BeanUtils.copyProperties(order, exitstingOrder, "_id", "addedAt", "modifiedAt");
        exitstingOrder.setModifiedAt(Instant.now());

        return orderRepository.save(exitstingOrder);
    }

    @Override
    public Order createOrder(Order order) {
        order.setOrderStatus(OrderStatus.PENDING);
        order.setAddedAt(Instant.now());
        order.setModifiedAt(Instant.now());

        Transaction tx = new Transaction();
        tx.setTransactionId(UUID.randomUUID().toString());
        tx.setStatus(TransactionStatus.PENDING);
        tx.setAmount(order.getTotalAmount());
        tx.setCreatedAt(Instant.now());

        Payment payment = new Payment();
        if (payment.getTransactions() == null) {
            payment.setMethod("cod");
            payment.setTransactions(new ArrayList<>());
        }
        payment.getTransactions().add(tx);

        order.setPayment(payment);
        return orderRepository.save(order);
    }

    @Override
    public Order patchOrder(String id, OrderPatchDTO dto) {
        Order existing = orderRepository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (dto.getOrderStatus() != null) {
            existing.setOrderStatus(dto.getOrderStatus());
        }
        if (dto.getShippingInformation() != null) {
            existing.setShippingInformation(dto.getShippingInformation());
        }
        if (dto.getPayment() != null) {
            existing.setPayment(dto.getPayment());
        }
        if (dto.getItems() != null) {
            existing.setItems(dto.getItems());
            double total = existing.getItems().stream()
                    .mapToDouble(i -> i.getPrice() * i.getQuantity())
                    .sum();
            existing.setTotalAmount(total);
        }
        if (dto.getDoneAt() != null) {
            existing.setDoneAt(dto.getDoneAt());
        }

        existing.setModifiedAt(Instant.now());
        return orderRepository.save(existing);
    }

    @Override
    public boolean setCompleteOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getOrderStatus().equals(OrderStatus.DELIVERED)) {
            return false;
        }
        order.setOrderStatus(OrderStatus.valueOf("COMPLETED"));
        order.setModifiedAt(Instant.now());
        orderRepository.save(order);
        return true;

    }

    @Override
    public boolean setCancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));


        if (order.getOrderStatus().equals(OrderStatus.PENDING) || order.getOrderStatus().equals(OrderStatus.PROCESSING)) {

            order.setOrderStatus(OrderStatus.valueOf("CANCELLED"));
            order.setModifiedAt(Instant.now());
            orderRepository.save(order);
            return true;
        }
        return false;

    }

    @Override
    public boolean setProcessOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getOrderStatus().equals(OrderStatus.PENDING)) {
            return false;
        }
        order.setOrderStatus(OrderStatus.valueOf("PROCESSING"));
        order.setModifiedAt(Instant.now());

        orderRepository.save(order);
        return true;
    }

    @Override
    public boolean setShipOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getOrderStatus().equals(OrderStatus.PROCESSING)) {
            return false;
        }
        order.setOrderStatus(OrderStatus.valueOf("SHIPPED"));
        order.setModifiedAt(Instant.now());
        orderRepository.save(order);
        return true;
    }

    @Override
    public boolean setDeliveredOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getOrderStatus().equals(OrderStatus.SHIPPED)) {
            return false;
        }

        order.setOrderStatus(OrderStatus.valueOf("DELIVERED"));
        order.setModifiedAt(Instant.now());

        Transaction tx = new Transaction();
        tx.setStatus(TransactionStatus.SUCCESSFUL);
        tx.setRawResponse("Payment successful");
        return test(order, tx);
    }

    @Override
    public boolean setPendingRefundOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getOrderStatus().equals(OrderStatus.DELIVERED)) {
            return false;
        }
        order.setOrderStatus(OrderStatus.valueOf("PENDING_REFUND"));
        order.setModifiedAt(Instant.now());

        Transaction tx = new Transaction();
        tx.setStatus(TransactionStatus.PENDING_REFUND);
        tx.setRawResponse("Pending refund...");
        return test(order, tx);
    }

    @Override
    public boolean setRefundedOrder(String orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        if (!order.getOrderStatus().equals(OrderStatus.PENDING_REFUND)) {
            return false;
        }
        order.setOrderStatus(OrderStatus.valueOf("REFUNDED"));
        order.setModifiedAt(Instant.now());

        Transaction tx = new Transaction();
        tx.setStatus(TransactionStatus.REFUNDED);
        tx.setRawResponse("Refund...");
        return test(order, tx);
    }

    private boolean test(Order order, Transaction tx) {
        tx.setTransactionId(UUID.randomUUID().toString());
        tx.setAmount(order.getTotalAmount());
        tx.setCreatedAt(Instant.now());

        Payment payment = order.getPayment();
        if (payment == null) {
            payment = new Payment();
            payment.setMethod("cod");
            payment.setTransactions(new ArrayList<>());
            order.setPayment(payment);
        }
        if (payment.getTransactions() == null) {
            payment.setTransactions(new ArrayList<>());
        }
        payment.getTransactions().add(tx);

        orderRepository.save(order);
        return true;
    }

    private MatchOperation buildMatch(TimeFrame timeFrame) {
        Criteria criteria = Criteria.where("orderstatus").is("COMPLETED");
        Instant start = timeFrame.getStartInstant();
        if (start != null) {
            criteria = criteria.and("doneAt").gte(start);
        }
        return match(criteria);
    }

}
