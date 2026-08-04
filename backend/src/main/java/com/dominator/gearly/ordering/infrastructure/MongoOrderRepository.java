package com.dominator.gearly.ordering.infrastructure;

import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderPage;
import com.dominator.gearly.ordering.domain.OrderQuery;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import com.dominator.gearly.shared.domain.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The MongoDB adapter behind {@link OrderRepository}. Plain CRUD delegates to Spring Data;
 * the customer order search builds criteria directly, which is what
 * {@code OrderRepositoryCustomImpl} did before it moved in here.
 *
 * <p>This is the only class in the context that knows an order is stored in MongoDB, that the
 * status is matched as a string, or that the buyer is a plain string on the document. The
 * conversions between the domain's typed ids and those raw values happen here and nowhere else.
 */
@Repository
@RequiredArgsConstructor
public class MongoOrderRepository implements OrderRepository {

    /** Newest activity first — the order the customer's list has always been shown in. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "modifiedAt");

    private final SpringDataOrderRepository orders;
    private final MongoTemplate mongoTemplate;
    private final ApplicationEventPublisher events;

    @Override
    public Optional<Order> findById(OrderId id) {
        return orders.findById(id.value());
    }

    /**
     * Writes the aggregate, then publishes whatever it recorded while it was being changed.
     *
     * <p>Publication lives here so that it happens in exactly one place: a use case cannot
     * forget to announce a change, and cannot announce one that failed to persist. Draining is
     * destructive, so an aggregate saved twice in a request does not announce itself twice.
     *
     * <p>After the write, not before — but still inside the caller's transaction, so a
     * {@code BEFORE_COMMIT} listener's work commits or rolls back together with the order.
     */
    @Override
    public Order save(Order order) {
        Order saved = orders.save(order);
        for (DomainEvent event : saved.pullDomainEvents()) {
            events.publishEvent(event);
        }
        return saved;
    }

    @Override
    public List<Order> findAll() {
        return orders.findAll();
    }

    @Override
    public long countByUserAndStatus(UserId userId, OrderStatus status) {
        return mongoTemplate.count(
                Query.query(ownedBy(userId).and("orderStatus").is(status.name())), Order.class);
    }

    @Override
    public long countByUserAndStatusNotIn(UserId userId, List<OrderStatus> statuses) {
        List<String> names = statuses.stream().map(Enum::name).toList();
        return mongoTemplate.count(
                Query.query(ownedBy(userId).and("orderStatus").nin(names)), Order.class);
    }

    /**
     * One page of a customer's orders.
     *
     * <p>Handles the no-filter, status-only and free-text cases through the same criteria,
     * where they used to be three separate repository methods that had drifted apart — the
     * status-only path matched the enum while the search path compared a raw request string,
     * so an unrecognized status meant a 500 on one route and an empty list on the other.
     */
    @Override
    public OrderPage findFor(OrderQuery orderQuery) {
        Query query = Query.query(ownedBy(orderQuery.userId()));

        if (orderQuery.hasStatus()) {
            query.addCriteria(Criteria.where("orderStatus").is(orderQuery.status().name()));
        }
        if (orderQuery.hasSearchTerm()) {
            query.addCriteria(searchAnywhere(orderQuery.searchTerm()));
        }

        long total = mongoTemplate.count(query, Order.class);
        List<Order> content = mongoTemplate.find(
                query.with(NEWEST_FIRST).skip((long) orderQuery.page() * orderQuery.size())
                        .limit(orderQuery.size()),
                Order.class);

        return new OrderPage(content, orderQuery.page(), orderQuery.size(), total);
    }

    /** The buyer is stored as a plain string; the typed id is unwrapped here and only here. */
    private Criteria ownedBy(UserId userId) {
        return Criteria.where("userId").is(userId.value());
    }

    /**
     * The customer's free-text search: the term against the order id, the line titles, every
     * field of the shipping details, the status, and — when it parses as a number — the total.
     */
    private Criteria searchAnywhere(String term) {
        String regex = ".*" + Pattern.quote(term) + ".*";
        List<Criteria> anyOf = new ArrayList<>();

        try {
            anyOf.add(Criteria.where("totalAmount").is(Double.parseDouble(term)));
        } catch (NumberFormatException notANumber) {
            // the term is not an amount; every other clause still applies
        }

        anyOf.add(Criteria.where("_id").is(term));
        anyOf.add(Criteria.where("items.title").regex(regex, "i"));
        anyOf.add(Criteria.where("shippingInformation.firstName").regex(regex, "i"));
        anyOf.add(Criteria.where("shippingInformation.lastName").regex(regex, "i"));

        // "Ada Lovelace" should match the two name fields in either order
        String[] nameParts = term.split("\\s+");
        if (nameParts.length >= 2) {
            String first = Pattern.quote(nameParts[0]);
            String second = Pattern.quote(nameParts[1]);
            anyOf.add(new Criteria().andOperator(
                    Criteria.where("shippingInformation.firstName").regex(first, "i"),
                    Criteria.where("shippingInformation.lastName").regex(second, "i")));
            anyOf.add(new Criteria().andOperator(
                    Criteria.where("shippingInformation.firstName").regex(second, "i"),
                    Criteria.where("shippingInformation.lastName").regex(first, "i")));
        }

        anyOf.add(Criteria.where("shippingInformation.email").regex(regex, "i"));
        anyOf.add(Criteria.where("shippingInformation.phoneNumber").regex(regex, "i"));
        anyOf.add(Criteria.where("shippingInformation.address.street").regex(regex, "i"));
        anyOf.add(Criteria.where("shippingInformation.address.city").regex(regex, "i"));
        anyOf.add(Criteria.where("shippingInformation.address.state").regex(regex, "i"));
        anyOf.add(Criteria.where("shippingInformation.address.postalCode").regex(regex, "i"));
        anyOf.add(Criteria.where("shippingInformation.address.country").regex(regex, "i"));
        anyOf.add(Criteria.where("orderStatus").regex(regex, "i"));

        return new Criteria().orOperator(anyOf.toArray(new Criteria[0]));
    }
}
