package com.dominator.gearly.ordering.api;

import com.dominator.gearly.ordering.domain.Order;
import org.springframework.stereotype.Component;

/**
 * Renders an {@link Order} as the JSON both frontends already expect.
 *
 * <p>Two things left this class in S10. {@code applyUpsert} used to copy an admin payload
 * field-by-field onto the entity through its setters, which made it one of the three paths
 * that could set a status without consulting the transition table; the admin write paths hand
 * a command to the aggregate now. And {@code toOrderItem} — the catalog snapshot — moved to
 * {@code PlaceOrderService}, because building an order line is a placement concern rather than
 * a rendering one, and because it is the seam S11 replaces with the {@code CatalogSnapshot}
 * anti-corruption layer.
 *
 * <p>What is left is a pure projection, which is all a mapper in the api layer should be.
 */
@Component
public class OrderResponseMapper {

    public OrderResponseDTO toResponseDto(Order order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setUserId(order.getUserId());
        dto.setItems(order.getItems());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPayment(order.getPayment());
        dto.setOrderStatus(order.getOrderStatus());
        dto.setShippingInformation(order.getShippingInformation());
        dto.setReviewed(order.isReviewed());
        dto.setNote(order.getNote());
        dto.setAddedAt(order.getAddedAt());
        dto.setModifiedAt(order.getModifiedAt());
        dto.setDoneAt(order.getDoneAt());
        return dto;
    }
}
