package com.dominator.bookify.security;

import com.dominator.bookify.config.CorsConfig;
import com.dominator.bookify.config.SecurityConfig;
import com.dominator.bookify.controller.admin.AdminOrderController;
import com.dominator.bookify.repository.UserRepository;
import com.dominator.bookify.service.admin.AdminOrderService;
import com.dominator.bookify.service.admin.OrderAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 1 verification: the admin surface is locked.
 *
 * <p>{@code /api/admin/**} now requires ROLE_ADMIN. This is a web-layer slice
 * (no MongoDB), so it exercises the real {@link SecurityConfig} filter chain
 * against anonymous / customer / admin principals. The JWT filter is a no-op
 * here (no Authorization header); roles come from {@code @WithMockUser}.
 */
@WebMvcTest(controllers = AdminOrderController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class AdminOrderSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AdminOrderService orderService;
    @MockBean
    private OrderAnalyticsService orderAnalyticsService;

    // Collaborators of the real JwtAuthenticationFilter (unused without a Bearer header).
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserRepository userRepository;

    @Test
    void anonymous_isRejected() throws Exception {
        mvc.perform(get("/api/admin/orders"))
                .andExpect(status().is4xxClientError()); // 401/403, never 200
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customer_isForbidden() throws Exception {
        mvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden()); // authenticated but lacks ADMIN
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void admin_isAllowed() throws Exception {
        mvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk());
    }
}
