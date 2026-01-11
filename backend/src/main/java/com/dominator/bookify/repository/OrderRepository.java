package com.dominator.bookify.repository;

import com.dominator.bookify.model.Order;
import com.dominator.bookify.model.OrderStatus;
import com.dominator.bookify.repository.custom.OrderRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String>, OrderRepositoryCustom {
    List<Order> findByUserId(String userId);
    Page<Order> findByUserId(String userId, Pageable pageable);

    Page<Order> findByUserIdAndOrderStatus(String userId, OrderStatus orderStatus, Pageable pageable);

    long countByUserIdAndOrderStatus(String userId, OrderStatus status);

    long countByUserIdAndOrderStatusNotIn(String userId, List<OrderStatus> statuses);
}