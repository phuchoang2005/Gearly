package com.dominator.gearly.ordering.api;

import com.dominator.gearly.platform.config.CorsConfig;
import com.dominator.gearly.platform.security.SecurityConfig;
import com.dominator.gearly.platform.exception.GlobalExceptionHandler;
import com.dominator.gearly.ordering.application.AdminOrderPatchCommand;
import com.dominator.gearly.ordering.application.AdminOrderService;
import com.dominator.gearly.ordering.domain.IllegalOrderTransitionException;
import com.dominator.gearly.ordering.domain.OrderStatus;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.platform.security.JwtAuthenticationFilter;
import com.dominator.gearly.identity.domain.AccessTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The S10 verification step, asserted through the real HTTP stack rather than through a
 * service call: <b>an illegal status transition is a 409 from every write path, and the seven
 * {@code set-*} endpoints keep their {@code 200 false} contract.</b>
 *
 * <p>Both halves matter and they pull in opposite directions, which is why they are pinned
 * together in one place. {@code PATCH} used to assign the status straight from the request
 * body, so {@code {"orderStatus":"REFUNDED"}} on a {@code PENDING} order moved it in one hop
 * with no error at all; it is a 409 now. The {@code set-*} endpoints have always answered
 * {@code 200 false} for a refused transition and the admin frontend reads that boolean, so
 * they still do — the aggregate throws and {@code AdminOrderService} catches.
 *
 * <p>A web slice, so the real {@code GlobalExceptionHandler} maps the exception and the real
 * security chain admits the request.
 */
@WebMvcTest(controllers = AdminOrderController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class, OrderResponseMapper.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
@WithMockUser(roles = "ADMIN")
class AdminOrderStatusEndpointTest {

    @Autowired
    private MockMvc mvc;

    @MockBean private AdminOrderService orderService;

    // Collaborators of the real JwtAuthenticationFilter (unused without a Bearer header).
    @MockBean private AccessTokens accessTokens;
    @MockBean private UserRepository userRepository;

    @Test
    @DisplayName("PATCH with a status the order cannot reach is a 409, not a silent assignment")
    void patchWithAnIllegalStatusIs409() throws Exception {
        when(orderService.patchOrder(eq("o1"), any(AdminOrderPatchCommand.class)))
                .thenThrow(new IllegalOrderTransitionException(OrderStatus.PENDING, OrderStatus.REFUNDED));

        mvc.perform(patch("/api/admin/orders/o1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderStatus\":\"REFUNDED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("An order cannot go from PENDING to REFUNDED"));
    }

    @Test
    @DisplayName("a refused set-* transition is still 200 false — the admin frontend reads that boolean")
    void refusedSetEndpointIs200False() throws Exception {
        when(orderService.transition("o1", OrderStatus.SHIPPED)).thenReturn(false);

        mvc.perform(post("/api/admin/orders/o1/set-ship"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }

    @Test
    void anAcceptedSetEndpointIs200True() throws Exception {
        when(orderService.transition("o1", OrderStatus.PROCESSING)).thenReturn(true);

        mvc.perform(post("/api/admin/orders/o1/set-process"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }
}
