package com.dominator.gearly.ordering.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The order state machine, now that it is a property of {@link OrderStatus} rather than a
 * private map on one of the four services that write a status.
 *
 * <p>These assertions are the same edges {@code AdminOrderServiceTest} exercised through the
 * service, restated where the rule now lives — plus the two things that map could not be
 * asked before: that every status is reachable, and that the final ones are dead ends.
 */
class OrderStatusTest {

    @Nested
    @DisplayName("the happy path")
    class HappyPath {

        @Test
        void pendingThroughToCompleted() {
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PROCESSING)).isTrue();
            assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.SHIPPED)).isTrue();
            assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
            assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.COMPLETED)).isTrue();
        }

        @Test
        @DisplayName("the fulfilment steps cannot be skipped")
        void stepsCannotBeSkipped() {
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
            assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.COMPLETED)).isFalse();
        }

        @Test
        @DisplayName("an order cannot go backwards")
        void noReversals() {
            assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PROCESSING)).isFalse();
            assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.SHIPPED)).isFalse();
            assertThat(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.DELIVERED)).isFalse();
        }
    }

    @Nested
    @DisplayName("cancellation and refunds")
    class CancellationAndRefunds {

        @Test
        void anUnstartedOrAcceptedOrderMayBeCancelled() {
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
            assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
        }

        @Test
        @DisplayName("once it has shipped it can no longer be cancelled")
        void aShippedOrderMayNotBeCancelled() {
            assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
            assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
        }

        /**
         * The reconciliation. The old {@code ALLOWED_SOURCES} listed {@code DELIVERED} as the
         * only source of {@code PENDING_REFUND}, while {@code cancelOrder} drove a paid
         * {@code PENDING}/{@code PROCESSING} order there — so the admin table forbade a
         * transition the customer path performed several times a day. The cancel path won.
         */
        @Test
        @DisplayName("a paid order awaits a refund whether or not it has been delivered")
        void pendingRefundIsReachableFromEveryPaidState() {
            assertThat(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING_REFUND)).isTrue();
            assertThat(OrderStatus.PROCESSING.canTransitionTo(OrderStatus.PENDING_REFUND)).isTrue();
            assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.PENDING_REFUND)).isTrue();
        }

        @Test
        @DisplayName("a refund must be pending before it can be settled")
        void refundedComesOnlyFromPendingRefund() {
            assertThat(OrderStatus.PENDING_REFUND.canTransitionTo(OrderStatus.REFUNDED)).isTrue();
            assertThat(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.REFUNDED)).isFalse();
            assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.REFUNDED)).isFalse();
        }

        @Test
        @DisplayName("a shipment in flight cannot be refunded either — it must be delivered first")
        void shippedIsNotARefundSource() {
            assertThat(OrderStatus.SHIPPED.canTransitionTo(OrderStatus.PENDING_REFUND)).isFalse();
        }
    }

    @Nested
    @DisplayName("structural properties of the table")
    class Structure {

        @ParameterizedTest
        @EnumSource(OrderStatus.class)
        @DisplayName("no status is a legal source for itself — re-issuing set-process is still refused")
        void noSelfTransition(OrderStatus status) {
            assertThat(status.canTransitionTo(status)).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class,
                names = {"CANCELLED", "REFUNDED", "COMPLETED"})
        @DisplayName("the final statuses are dead ends")
        void finalStatusesHaveNoTargets(OrderStatus terminal) {
            assertThat(terminal.allowedTargets()).isEmpty();
        }

        @Test
        @DisplayName("PROCESSING is the only way back to PENDING — the failed-payment reversal")
        void pendingIsReachableOnlyFromProcessing() {
            for (OrderStatus source : OrderStatus.values()) {
                assertThat(source.canTransitionTo(OrderStatus.PENDING))
                        .as("%s -> PENDING", source)
                        .isEqualTo(source == OrderStatus.PROCESSING);
            }
        }

        @Test
        @DisplayName("every status is reachable from somewhere — no orphans in the table")
        void everyStatusIsReachable() {
            for (OrderStatus target : OrderStatus.values()) {
                boolean reachable = false;
                for (OrderStatus source : OrderStatus.values()) {
                    reachable |= source.canTransitionTo(target);
                }
                assertThat(reachable).as("something reaches %s", target).isTrue();
            }
        }
    }

    @Nested
    @DisplayName("assertCanTransitionTo")
    class Assertions {

        @Test
        void aLegalMovePasses() {
            assertThatCode(() -> OrderStatus.PENDING.assertCanTransitionTo(OrderStatus.PROCESSING))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("an illegal move names both ends, so the 409 body says what was refused")
        void anIllegalMoveThrows() {
            assertThatThrownBy(() -> OrderStatus.PENDING.assertCanTransitionTo(OrderStatus.REFUNDED))
                    .isInstanceOf(IllegalOrderTransitionException.class)
                    .hasMessage("An order cannot go from PENDING to REFUNDED")
                    .satisfies(ex -> {
                        IllegalOrderTransitionException e = (IllegalOrderTransitionException) ex;
                        assertThat(e.getFrom()).isEqualTo(OrderStatus.PENDING);
                        assertThat(e.getTo()).isEqualTo(OrderStatus.REFUNDED);
                    });
        }
    }
}
