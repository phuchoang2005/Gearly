package com.dominator.bookify.repository.custom;

import com.dominator.bookify.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepositoryCustom {
    Page<Order> searchOrders(String userId, String status, String searchTerm, Pageable pageable);
}
