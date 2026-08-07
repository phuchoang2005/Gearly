package com.dominator.gearly.ordering.application;



import com.dominator.gearly.ordering.domain.OrderNotFoundException;
import com.dominator.gearly.ordering.domain.UnknownOrderStatusException;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderNotYoursException;
import com.dominator.gearly.ordering.domain.OrderPage;
import com.dominator.gearly.ordering.domain.OrderQuery;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.shared.domain.AccessDeniedDomainException;
import com.dominator.gearly.shared.domain.OrderId;
import com.dominator.gearly.shared.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * <b>Characterization suite (S8), carried forward</b> — the read half of what was
 * {@code CustomerOrderServiceTest}, plus the status-parsing the three collapsed query paths
 * now share. See {@link PlaceOrderServiceTest} on the split.
 */
@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {

    @Mock private OrderRepository orderRepository;

    private OrderQueryService service;

    private static final UserId USER_ID = UserId.of("user-1");

    @BeforeEach
    void setUp() {
        service = new OrderQueryService(orderRepository);
    }

    private OrderQuery captureQuery() {
        ArgumentCaptor<OrderQuery> captor = ArgumentCaptor.forClass(OrderQuery.class);
        verify(orderRepository).findFor(captor.capture());
        return captor.getValue();
    }

    // ---- counts ------------------------------------------------------------

    @Test
    @DisplayName("countByStatus reports every status plus a totalInProgress roll-up")
    void includesEveryStatusAndTotalInProgress() {
        for (OrderStatus status : OrderStatus.values()) {
            when(orderRepository.countByUserAndStatus(USER_ID, status)).thenReturn(1L);
        }
        when(orderRepository.countByUserAndStatusNotIn(eq(USER_ID), any())).thenReturn(5L);

        Map<String, Long> counts = service.countByStatus(USER_ID);

        assertThat(counts).hasSize(OrderStatus.values().length + 1);
        assertThat(counts).containsEntry("PENDING", 1L).containsEntry("totalInProgress", 5L);
    }

    // ---- findById ----------------------------------------------------------

    @Test
    void findById_returnsTheCallersOwnOrder() {
        Order order = OrderFixture.anOrder().withId("order-1").ownedBy(USER_ID.value()).build();
        when(orderRepository.findById(OrderId.of("order-1"))).thenReturn(Optional.of(order));

        assertThat(service.findById(USER_ID, "order-1")).isSameAs(order);
    }

    /**
     * <b>The S12 IDOR.</b> Until this sprint {@code findById} took an id and nothing else, so
     * this call returned somebody else's order in full — delivery address, phone number and
     * payment ledger included — to any authenticated caller who guessed an id. The assertion
     * is on the type rather than on a status because the domain does not name one;
     * {@code GlobalExceptionHandler} maps {@code AccessDeniedDomainException} to 403, and
     * {@code CustomerOrderAccessTest} asserts that end of it through the real HTTP stack.
     */
    @Test
    void findById_someoneElsesOrder_isRefused() {
        Order order = OrderFixture.anOrder().withId("order-1").ownedBy("someone-else").build();
        when(orderRepository.findById(OrderId.of("order-1"))).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.findById(USER_ID, "order-1"))
                .isInstanceOf(OrderNotYoursException.class)
                .isInstanceOf(AccessDeniedDomainException.class);
    }

    @Test
    void findById_missing_is404() {
        when(orderRepository.findById(OrderId.of("nope"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(USER_ID, "nope"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    // ---- search ------------------------------------------------------------

    @Test
    @DisplayName("the query is scoped to the caller and carries the page size the list has always used")
    void buildsAScopedQuery() {
        when(orderRepository.findFor(any())).thenReturn(new OrderPage(List.of(), 0, 10, 0));

        service.search(USER_ID, null, null, 3);

        OrderQuery query = captureQuery();
        assertThat(query.userId()).isEqualTo(USER_ID);
        assertThat(query.page()).isEqualTo(3);
        assertThat(query.size()).isEqualTo(10);
        assertThat(query.hasStatus()).isFalse();
        assertThat(query.hasSearchTerm()).isFalse();
    }

    @Test
    @DisplayName("a blank status or search term means no filter, not a filter on the empty string")
    void blankFiltersAreNoFilters() {
        when(orderRepository.findFor(any())).thenReturn(new OrderPage(List.of(), 0, 10, 0));

        service.search(USER_ID, "   ", "  ", 0);

        OrderQuery query = captureQuery();
        assertThat(query.hasStatus()).isFalse();
        assertThat(query.hasSearchTerm()).isFalse();
    }

    @Test
    void aKnownStatusIsPassedThroughAsTheEnum() {
        when(orderRepository.findFor(any())).thenReturn(new OrderPage(List.of(), 0, 10, 0));

        service.search(USER_ID, null, "PROCESSING", 0);

        assertThat(captureQuery().status()).isEqualTo(OrderStatus.PROCESSING);
    }

    /**
     * Deliberate change. The status parameter used to be read two different ways: the
     * status-only path called {@code OrderStatus.valueOf} and produced a 500 on a bad value,
     * while the search path passed the raw string to a comparison against the stored enum and
     * produced an empty list. One parse, one answer.
     */
    @Test
    @DisplayName("an unrecognized status is a 400 rather than a 500 or a silently empty history")
    void anUnknownStatusIsABadRequest() {
        assertThatThrownBy(() -> service.search(USER_ID, null, "BOGUS", 0))
                .isInstanceOf(UnknownOrderStatusException.class)
                .hasMessageContaining("BOGUS");

        assertThatThrownBy(() -> service.search(USER_ID, "lovelace", "BOGUS", 0))
                .as("the same answer whether or not a search term accompanies it")
                .isInstanceOf(UnknownOrderStatusException.class);

        verifyNoInteractions(orderRepository);
    }

    @Test
    @DisplayName("the page is rebuilt with the total count, sorted newest first")
    void rebuildsTheSpringPage() {
        Order order = OrderFixture.anOrder().withId("order-1").build();
        when(orderRepository.findFor(any())).thenReturn(new OrderPage(List.of(order), 1, 10, 42));

        Page<Order> page = service.search(USER_ID, null, null, 1);

        assertThat(page.getContent()).containsExactly(order);
        assertThat(page.getTotalElements()).isEqualTo(42);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getSort().getOrderFor("modifiedAt")).isNotNull();
        assertThat(page.getSort().getOrderFor("modifiedAt").isDescending()).isTrue();
    }
}
