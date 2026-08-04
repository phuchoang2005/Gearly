package com.dominator.gearly.ordering.infrastructure;

import com.dominator.gearly.ordering.domain.Order;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * The Spring Data repository {@link MongoOrderRepository} delegates the plain CRUD to.
 *
 * <p>Deliberately bare. Every query this context needs beyond find/save/findAll is expressed
 * as criteria in the adapter, because the derived-query DSL cannot build the customer order
 * search — that one is a dozen optional regex clauses OR'd together. Splitting it across a
 * derived query for the simple cases and criteria for the hard one is what produced three
 * subtly different code paths for the same list before; there is one now.
 *
 * <p>Package-private: nothing outside this package may hold a Spring Data repository for an
 * order. Ordering's dependency is on the {@code OrderRepository} port.
 */
interface SpringDataOrderRepository extends MongoRepository<Order, String> {
}
