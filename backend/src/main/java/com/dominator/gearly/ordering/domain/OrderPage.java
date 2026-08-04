package com.dominator.gearly.ordering.domain;

import java.util.List;

/**
 * One page of orders: the slice itself plus what the caller needs to describe it.
 *
 * <p>This context's own paging type rather than Spring Data's {@code Page}, because the
 * domain may not name {@code org.springframework.data.domain..}. The application layer turns
 * it into a {@code PageImpl} at the HTTP edge, which is what keeps the JSON the frontends
 * receive byte-identical.
 */
public record OrderPage(List<Order> content, int page, int size, long totalElements) {

    public OrderPage {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static OrderPage empty(OrderQuery query) {
        return new OrderPage(List.of(), query.page(), query.size(), 0);
    }
}
