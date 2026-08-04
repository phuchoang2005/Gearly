package com.dominator.gearly.mapper;

import com.dominator.gearly.dto.OrderResponseDTO;
import com.dominator.gearly.model.Image;
import com.dominator.gearly.model.Product;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderLine;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.shared.domain.Money;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The catalog-to-order-line snapshot, and the response view.
 *
 * <p>{@code applyUpsert} used to be tested here. It is gone — copying an admin payload onto an
 * order is {@code Order.replaceContent} now, so that coverage lives in
 * {@code AdminOrderServiceTest} where the rules it has to respect can be asserted alongside it.
 */
class OrderMapperTest {

    private final OrderMapper mapper = new OrderMapper();

    @Test
    void toOrderLine_snapshotsPriceTitleImageAndQuantity() {
        Product product = new Product();
        product.setId("p1");
        product.setTitle("RTX 4090");
        product.setPrice(Money.of(1599.0));
        product.setImages(List.of(new Image("http://img/first.png", "gpu"),
                new Image("http://img/second.png", "gpu-back")));

        OrderLine line = mapper.toOrderLine(product, 3);

        assertThat(line.getProductId().value()).isEqualTo("p1");
        assertThat(line.getTitle()).isEqualTo("RTX 4090");
        assertThat(line.getPrice()).isEqualTo(Money.of(1599.0));
        assertThat(line.getQuantity().toInt()).isEqualTo(3);
        // snapshot uses the product's first image
        assertThat(line.getImageUrl()).isEqualTo("http://img/first.png");
    }

    @Test
    @DisplayName("a line knows what it costs, so the total never has to be computed by hand")
    void orderLine_knowsItsOwnTotal() {
        OrderLine line = OrderFixture.line("p1", "GPU", 10.50, 3);

        assertThat(line.lineTotal()).isEqualTo(Money.of(31.50));
    }

    @Test
    void toResponseDto_mirrorsEveryFieldOfTheAggregate() {
        Order order = OrderFixture.anOrder()
                .ownedBy("u1")
                .withLines(OrderFixture.line("p1", "GPU", 10.0, 2))
                .withNote("gift wrap")
                .reviewed()
                .doneAt(Instant.parse("2026-01-04T03:04:05Z"))
                .persistedAs("o1",
                        Instant.parse("2026-01-02T03:04:05Z"),
                        Instant.parse("2026-01-03T03:04:05Z"))
                .build();

        OrderResponseDTO dto = mapper.toResponseDto(order);

        assertThat(dto.getId()).isEqualTo("o1");
        assertThat(dto.getUserId()).isEqualTo(UserId.of("u1"));
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getTotalAmount()).isEqualTo(order.getTotalAmount());
        assertThat(dto.getPayment()).isSameAs(order.getPayment());
        assertThat(dto.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(dto.getShippingInformation()).isSameAs(order.getShippingInformation());
        assertThat(dto.isReviewed()).isTrue();
        assertThat(dto.getNote()).isEqualTo("gift wrap");
        assertThat(dto.getAddedAt()).isEqualTo(Instant.parse("2026-01-02T03:04:05Z"));
        assertThat(dto.getModifiedAt()).isEqualTo(Instant.parse("2026-01-03T03:04:05Z"));
        assertThat(dto.getDoneAt()).isEqualTo(Instant.parse("2026-01-04T03:04:05Z"));
    }
}
