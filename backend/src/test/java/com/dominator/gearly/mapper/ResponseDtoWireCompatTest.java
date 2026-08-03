package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.CartResponseDTO;
import com.dominator.gearly.dto.OrderResponseDTO;
import com.dominator.gearly.dto.ProductResponseDTO;
import com.dominator.gearly.model.Cart;
import com.dominator.gearly.model.CartItem;
import com.dominator.gearly.model.Image;
import com.dominator.gearly.model.Order;
import com.dominator.gearly.model.OrderItem;
import com.dominator.gearly.model.OrderStatus;
import com.dominator.gearly.model.Payment;
import com.dominator.gearly.model.Product;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the S7 "entity -> response DTO" change: each response DTO must serialize
 * to exactly the same JSON as the entity it replaced, so neither frontend sees a
 * wire change. Uses the app's auto-configured {@link ObjectMapper} and compares
 * the parsed JSON trees (order-insensitive).
 */
@JsonTest
class ResponseDtoWireCompatTest {

    @Autowired
    private ObjectMapper json;

    private final OrderMapper orderMapper = new OrderMapper();
    private final CartMapper cartMapper = new CartMapper();
    private final ProductMapper productMapper = new ProductMapper();

    @Test
    void orderResponseDto_matchesEntityWire() {
        Order order = new Order();
        order.setId("o1");
        order.setUserId("u1");
        order.setItems(List.of(new OrderItem("p1", "RTX 4090", 1599.0, "http://img/a.png", 2)));
        order.setTotalAmount(3198.0);
        order.setPayment(new Payment("MOMO", List.of()));
        order.setOrderStatus(OrderStatus.PENDING);
        order.setReviewed(true);
        order.setNote("leave at door");
        order.setAddedAt(Instant.parse("2026-01-02T03:04:05Z"));
        order.setModifiedAt(Instant.parse("2026-01-03T03:04:05Z"));
        order.setDoneAt(Instant.parse("2026-01-04T03:04:05Z"));

        OrderResponseDTO dto = orderMapper.toResponseDto(order);

        JsonNode dtoNode = json.valueToTree(dto);
        JsonNode entityNode = json.valueToTree(order);
        assertThat(dtoNode).isEqualTo(entityNode);
    }

    @Test
    void cartResponseDto_matchesEntityWire() {
        Cart cart = new Cart();
        cart.setId("c1");
        cart.setUserId("u1");
        cart.setGuestId(null);
        cart.setItems(List.of(new CartItem("p1", "RTX 4090", "NVIDIA", 1599.0, 1, "http://img/a.png", "NEW", 5)));
        cart.setCreatedAt(new Date(1_700_000_000_000L));
        cart.setUpdatedAt(new Date(1_700_000_100_000L));

        CartResponseDTO dto = cartMapper.toResponseDto(cart);

        JsonNode dtoNode = json.valueToTree(dto);
        JsonNode entityNode = json.valueToTree(cart);
        assertThat(dtoNode).isEqualTo(entityNode);
    }

    @Test
    void productResponseDto_matchesEntityWire() {
        Product product = new Product();
        product.setId("p1");
        product.setTitle("RTX 4090");
        product.setAuthors(List.of("NVIDIA"));
        product.setDescription("flagship GPU");
        product.setPrice(1599.0);
        product.setOriginalPrice(1799.0);
        product.setCondition("NEW");
        product.setStock(5);
        product.setCategoryIds(List.of(new ObjectId("64b7f0000000000000000001")));
        product.setImages(List.of(new Image("http://img/a.png", "gpu")));
        product.setCategoryNames(List.of("Graphics Cards"));
        product.setAverageRating(4.5);
        product.setRatingCount(10);
        product.setTotalRating(45);
        product.setAddedAt(Instant.parse("2026-01-02T03:04:05Z"));
        product.setModifiedAt(Instant.parse("2026-01-03T03:04:05Z"));

        ProductResponseDTO dto = productMapper.toResponseDto(product);

        JsonNode dtoNode = json.valueToTree(dto);
        JsonNode entityNode = json.valueToTree(product);
        assertThat(dtoNode).isEqualTo(entityNode);
    }
}
