package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.OrderResponseDTO;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.shared.domain.ProductId;
import com.dominator.gearly.shared.domain.Quantity;
import org.springframework.stereotype.Component;

/**
 * Builds {@link OrderLine} snapshots from a {@link Product} at order-placement time,
 * capturing the price and title as they were when the order was made, and renders an
 * {@link Order} as its response view.
 *
 * <p>{@code applyUpsert} is gone. It used to copy the admin payload field-by-field onto the
 * entity through its setters, which made it one of the three paths that could set a status
 * without consulting the transition table. The admin write paths call
 * {@code Order.replaceContent} / {@code Order.amend} now, so the aggregate applies the
 * payload and enforces its own rules while doing it.
 *
 * <p>{@code toOrderLine} is the seam S11 replaces: once Catalog publishes a
 * {@code CatalogSnapshot}, Ordering stops naming {@code Product} at all and this becomes
 * {@code OrderLine.fromSnapshot(...)}. Its unguarded {@code getImages().getFirst()} — a
 * crash on any image-less product — is S11's to fix along with it.
 */
@Component
public class OrderMapper {

    public OrderLine toOrderLine(Product product, int quantity) {
        return new OrderLine(
                ProductId.of(product.getId()),
                product.getTitle(),
                product.getPrice(),
                product.getImages().getFirst().getUrl(),
                Quantity.of(quantity));
    }

    /** Response view mirroring the aggregate's wire shape. */
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
