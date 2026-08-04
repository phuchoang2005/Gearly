package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.OrderUpsertRequestDTO;
import com.dominator.gearly.model.Image;
import com.dominator.gearly.model.Order;
import com.dominator.gearly.model.OrderItem;
import com.dominator.gearly.model.OrderStatus;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void toOrderItem_snapshotsPriceTitleImageAndQuantity() {
        Product product = new Product();
        product.setId("p1");
        product.setTitle("RTX 4090");
        product.setPrice(Money.of(1599.0));
        product.setImages(List.of(new Image("http://img/first.png", "gpu"),
                new Image("http://img/second.png", "gpu-back")));

        OrderItem item = mapper.toOrderItem(product, 3);

        assertThat(item.getProductId()).isEqualTo("p1");
        assertThat(item.getTitle()).isEqualTo("RTX 4090");
        assertThat(item.getPrice()).isEqualTo(Money.of(1599.0));
        assertThat(item.getQuantity()).isEqualTo(3);
        // snapshot uses the product's first image
        assertThat(item.getImageUrl()).isEqualTo("http://img/first.png");
    }

    @Test
    void applyUpsert_copiesAdminSettableFields_leavesIdAndTimestamps() {
        Order existing = new Order();
        existing.setId("o1");
        existing.setAddedAt(Instant.parse("2020-01-01T00:00:00Z"));
        existing.setModifiedAt(Instant.parse("2020-01-01T00:00:00Z"));

        OrderUpsertRequestDTO dto = new OrderUpsertRequestDTO();
        dto.setUserId("u1");
        dto.setItems(List.of(new OrderItem("p1", "GPU", Money.of(10.0), "http://img", 2)));
        dto.setTotalAmount(Money.of(20.0));
        dto.setOrderStatus(OrderStatus.PROCESSING);
        dto.setReviewed(true);
        dto.setNote("gift wrap");
        dto.setDoneAt(Instant.parse("2026-01-01T00:00:00Z"));

        mapper.applyUpsert(existing, dto);

        // admin-settable fields copied
        assertThat(existing.getUserId()).isEqualTo("u1");
        assertThat(existing.getItems()).hasSize(1);
        assertThat(existing.getTotalAmount()).isEqualTo(Money.of(20.0));
        assertThat(existing.getOrderStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(existing.isReviewed()).isTrue();
        assertThat(existing.getNote()).isEqualTo("gift wrap");
        assertThat(existing.getDoneAt()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        // managed fields untouched by the mapper
        assertThat(existing.getId()).isEqualTo("o1");
        assertThat(existing.getAddedAt()).isEqualTo(Instant.parse("2020-01-01T00:00:00Z"));
        assertThat(existing.getModifiedAt()).isEqualTo(Instant.parse("2020-01-01T00:00:00Z"));
    }
}
