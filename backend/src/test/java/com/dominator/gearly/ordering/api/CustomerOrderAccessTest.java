package com.dominator.gearly.ordering.api;

import com.dominator.gearly.platform.config.CorsConfig;
import com.dominator.gearly.exception.GlobalExceptionHandler;
import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.UserFixture;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.ordering.application.CancelOrderService;
import com.dominator.gearly.ordering.application.OnlinePaymentService;
import com.dominator.gearly.ordering.application.OrderQueryService;
import com.dominator.gearly.ordering.application.PlaceOrderService;
import com.dominator.gearly.ordering.domain.Order;
import com.dominator.gearly.ordering.domain.OrderFixture;
import com.dominator.gearly.ordering.domain.OrderRepository;
import com.dominator.gearly.platform.security.AuthenticatedUser;
import com.dominator.gearly.platform.security.JwtAuthenticationFilter;
import com.dominator.gearly.platform.security.SecurityConfig;
import com.dominator.gearly.shared.domain.OrderId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <b>The S12 IDOR, asserted through the real HTTP stack.</b>
 *
 * <p>{@code GET /api/orders/{id}} had no ownership check: it was authenticated, so the security
 * chain let it through, and then it returned whatever order the id named — delivery address,
 * phone number and payment ledger included — to any customer who guessed one.
 *
 * <p>This is asserted here rather than only in {@code OrderQueryServiceTest} because the fix has
 * two halves that a service-level test cannot see together: the controller has to unwrap the
 * principal and pass it, and {@code GlobalExceptionHandler} has to turn the refusal into a 403
 * rather than the 500 an unmapped exception produces. Both were missing; either one still
 * missing looks like a passing unit test and a broken endpoint. So the real
 * {@link OrderQueryService} runs here with only the repository mocked, and the assertion is on
 * the status code and body the storefront actually receives.
 *
 * <p>A web slice, so the real {@link SecurityConfig} chain and the real
 * {@link GlobalExceptionHandler} are both in the path.
 */
@WebMvcTest(controllers = OrderController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class, OrderResponseMapper.class, OrderQueryService.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class CustomerOrderAccessTest {

    @Autowired
    private MockMvc mvc;

    /** The real OrderQueryService is imported; only its port is mocked. */
    @MockBean private OrderRepository orderRepository;

    @MockBean private PlaceOrderService placeOrderService;
    @MockBean private CancelOrderService cancelOrderService;
    @MockBean private OnlinePaymentService onlinePaymentService;

    // Collaborators of the real JwtAuthenticationFilter (unused without a Bearer header).
    @MockBean private AccessTokens accessTokens;
    @MockBean private UserRepository userRepository;

    private static final String ADA = "user-ada";
    private static final String GRACE = "user-grace";

    private AuthenticatedUser signedInAs(String userId) {
        return new AuthenticatedUser(UserFixture.aUser().withId(userId)
                .withEmail(userId + "@example.com").build());
    }

    private void ordersCollectionHolds(String orderId, String owner) {
        Order order = OrderFixture.anOrder().withId(orderId).ownedBy(owner)
                .withLines(OrderFixture.line("p1", "RTX 4090", 1599.0, 1))
                .build();
        when(orderRepository.findById(OrderId.of(orderId))).thenReturn(Optional.of(order));
    }

    @Test
    @DisplayName("reading somebody else's order is 403, not the order")
    void anotherCustomersOrderIs403() throws Exception {
        ordersCollectionHolds("order-1", ADA);

        mvc.perform(get("/api/orders/order-1").with(user(signedInAs(GRACE))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("You are not allowed to view this order"))
                // the point of the fix: none of it comes back
                .andExpect(jsonPath("$.items").doesNotExist())
                .andExpect(jsonPath("$.shippingInformation").doesNotExist());
    }

    @Test
    @DisplayName("reading your own order still works")
    void yourOwnOrderIs200() throws Exception {
        ordersCollectionHolds("order-1", ADA);

        mvc.perform(get("/api/orders/order-1").with(user(signedInAs(ADA))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("order-1"));
    }

    @Test
    @DisplayName("an order that does not exist is still 404, and says nothing about ownership")
    void anUnknownOrderIs404() throws Exception {
        when(orderRepository.findById(OrderId.of("nope"))).thenReturn(Optional.empty());

        mvc.perform(get("/api/orders/nope").with(user(signedInAs(ADA))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Order not found"));
    }

    @Test
    @DisplayName("an anonymous read is refused by the chain before any of this")
    void anonymousIsRefused() throws Exception {
        mvc.perform(get("/api/orders/order-1"))
                .andExpect(status().is4xxClientError());
    }
}
