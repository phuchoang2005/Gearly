package com.dominator.gearly.ordering.application;

import com.dominator.gearly.exception.BadRequestException;
import com.dominator.gearly.exception.ResourceNotFoundException;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderPage;
import com.dominator.gearly.ordering.domain.OrderQuery;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The customer-facing reads over their own order history. */
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    /** How many orders one page of a customer's history shows. */
    private static final int PAGE_SIZE = 10;

    /** Newest activity first — the order the customer's list has always been shown in. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "modifiedAt");

    /** Statuses an order is finished in, and so is no longer "in progress". */
    private static final List<OrderStatus> FINAL_STATUSES = List.of(
            OrderStatus.COMPLETED, OrderStatus.CANCELLED, OrderStatus.REFUNDED);

    private final OrderRepository orderRepository;

    /**
     * One order by id.
     *
     * <p><b>No ownership check</b>, which is the S12 IDOR fix, not this sprint's: any
     * authenticated caller can still read any order, payment details and delivery address
     * included. It is called out here so the gap is visible in the code that has it rather
     * than only in the plan. The aggregate already exposes {@code isOwnedBy}, so closing it is
     * one line plus a decision about what the admin frontend relies on.
     */
    public Order findById(String orderId) {
        return orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    /**
     * One page of the caller's own order history.
     *
     * <p>The {@code Page} is rebuilt here, at the boundary, from the context's own
     * {@code OrderPage}: the domain may not name a Spring Data paging type, and the JSON the
     * frontends receive has to stay exactly as it was.
     */
    public Page<Order> search(UserId userId, String searchTerm, String status, int page) {
        OrderPage result = orderRepository.findFor(
                new OrderQuery(userId, parseStatus(status), searchTerm, page, PAGE_SIZE));

        return new PageImpl<>(
                result.content(),
                PageRequest.of(result.page(), result.size(), NEWEST_FIRST),
                result.totalElements());
    }

    public Map<String, Long> countByStatus(UserId userId) {
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (OrderStatus status : OrderStatus.values()) {
            statusCounts.put(status.name(), orderRepository.countByUserAndStatus(userId, status));
        }
        statusCounts.put("totalInProgress",
                orderRepository.countByUserAndStatusNotIn(userId, FINAL_STATUSES));
        return statusCounts;
    }

    /**
     * Blank means "no filter", as it always has.
     *
     * <p>An unrecognized value is a 400. It used to be a 500 or an empty list depending on
     * whether a search term happened to accompany it, because the two query paths disagreed
     * about how to interpret the parameter. Same treatment the {@code condition} filter got in
     * S9, and the same reason: telling the caller their filter is wrong beats showing them an
     * empty history.
     */
    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim());
        } catch (IllegalArgumentException unknown) {
            throw new BadRequestException("Unknown order status: " + status);
        }
    }
}
