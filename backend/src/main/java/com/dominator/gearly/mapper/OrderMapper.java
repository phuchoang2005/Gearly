package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.OrderUpsertRequestDTO;
import com.dominator.gearly.model.Order;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.model.OrderItem;
import org.springframework.stereotype.Component;

/**
 * Builds {@link OrderItem} snapshots from a {@link Product} at order-placement time,
 * capturing the price and title as they were when the order was made, and applies
 * admin upsert payloads onto {@link Order} entities.
 */
@Component
public class OrderMapper {

    public OrderItem toOrderItem(Product product, int quantity) {
        OrderItem item = new OrderItem();
        item.setProductId(product.getId());
        item.setImageUrl(product.getImages().getFirst().getUrl());
        item.setTitle(product.getTitle());
        item.setPrice(product.getPrice());
        item.setQuantity(quantity);
        return item;
    }

    /**
     * Copies the admin-settable fields from {@code dto} onto {@code target}. The
     * order id and audit timestamps are managed by the service and never taken
     * from the request. Mirrors the fields the old raw-entity bind wrote.
     */
    public void applyUpsert(Order target, OrderUpsertRequestDTO dto) {
        target.setUserId(dto.getUserId());
        target.setItems(dto.getItems());
        target.setTotalAmount(dto.getTotalAmount());
        target.setPayment(dto.getPayment());
        target.setOrderStatus(dto.getOrderStatus());
        target.setShippingInformation(dto.getShippingInformation());
        target.setReviewed(dto.isReviewed());
        target.setNote(dto.getNote());
        target.setDoneAt(dto.getDoneAt());
    }
}
