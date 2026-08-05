package com.dominator.gearly.ordering.domain;

import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;

import java.util.List;
import java.util.Optional;

/**
 * The port through which Ordering stores and retrieves its aggregate. Declared here, in the
 * domain, and implemented by {@code MongoOrderRepository} out in {@code infrastructure} —
 * dependencies point inward, so the domain names what it needs and nothing about how.
 *
 * <h2>Why it does not extend {@code MongoRepository}</h2>
 * Because that would put the persistence technology in the domain's vocabulary: the interface
 * would inherit forty methods nobody asked for, {@code Page}/{@code Pageable}/{@code Sort}
 * would leak in with them, and ArchUnit's {@code domain_is_free_of_framework_types} —which
 * bans {@code org.springframework.data.mongodb.repository..} and
 * {@code org.springframework.data.domain..} in a domain package — would fail. That ban is the
 * rule, not an obstacle to it: this interface has to be implementable by an in-memory fake in
 * a unit test, and a Spring Data one is not.
 *
 * <p>{@link OrderQuery} and {@link OrderPage} exist for the same reason: they are this
 * context's own paging vocabulary, so the application layer can build a Spring {@code Page}
 * for the HTTP edge without the domain ever naming one.
 */
public interface OrderRepository {

    Optional<Order> findById(OrderId id);

    Order save(Order order);

    /** Every order, unpaged — the admin list. */
    List<Order> findAll();

    /** One page of a customer's orders, filtered and searched per {@code query}. */
    OrderPage findFor(OrderQuery query);

    long countByUserAndStatus(UserId userId, OrderStatus status);

    long countByUserAndStatusNotIn(UserId userId, List<OrderStatus> statuses);
}
