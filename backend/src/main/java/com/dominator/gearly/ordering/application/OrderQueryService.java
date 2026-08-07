package com.dominator.gearly.ordering.application;

import com.dominator.gearly.ordering.domain.UnknownOrderStatusException;
import com.dominator.gearly.ordering.domain.OrderNotFoundException;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderNotYoursException;
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
     * One of the caller's own orders, by id.
     *
     * <p><b>The ownership check is the S12 IDOR fix.</b> Until this sprint this method took an
     * id and nothing else, so any authenticated customer could read any order by guessing an
     * id — delivery address, phone number and payment ledger included. The caller is a
     * parameter now, so the check cannot be forgotten at a call site: there is no signature
     * that omits it.
     *
     * <p>The admin console does not come through here. Its refine dataProvider is based at
     * {@code /api/admin} ({@code frontend_admin/gearly/src/App.tsx}), so its {@code orders}
     * resource resolves to {@code /api/admin/orders/{id}} — a separate endpoint on
     * {@code AdminOrderController}, which is where "any order" is the correct answer and
     * {@code @PreAuthorize} is what grants it. The only caller of this route is the
     * storefront's {@code orderService.js}.
     */
    public Order findById(UserId caller, String orderId) {
        Order order = orderRepository.findById(OrderId.of(orderId))
                .orElseThrow(() -> new OrderNotFoundException());

        if (!order.isOwnedBy(caller)) {
            throw OrderNotYoursException.toView();
        }
        return order;
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
            throw new UnknownOrderStatusException(status);
        }
    }
}
