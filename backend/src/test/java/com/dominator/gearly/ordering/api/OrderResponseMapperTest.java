package com.dominator.gearly.ordering.api;

import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The response projection.
 *
 * <p>Two things used to be tested here and are asserted elsewhere now, because they left this
 * class: {@code applyUpsert} — copying an admin payload onto an order — is
 * {@code Order.replaceContent}, covered by {@code AdminOrderServiceTest} where the rules it
 * has to respect can be asserted alongside it; and the catalog snapshot is
 * {@code PlaceOrderService}'s, covered by {@code PlaceOrderServiceTest.snapshotsCatalogFields}.
 */
class OrderResponseMapperTest {

    private final OrderResponseMapper mapper = new OrderResponseMapper();

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
