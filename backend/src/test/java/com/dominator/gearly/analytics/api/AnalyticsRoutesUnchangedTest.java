package com.dominator.gearly.analytics.api;

import com.dominator.gearly.analytics.application.DashboardCustomerQuery;
import com.dominator.gearly.analytics.application.DashboardProductQuery;
import com.dominator.gearly.analytics.application.SalesAnalyticsQuery;
import com.dominator.gearly.catalog.api.AdminLowStockController;
import com.dominator.gearly.catalog.application.ProductQueryService;
import com.dominator.gearly.ordering.api.AdminOrderController;
import com.dominator.gearly.ordering.api.OrderResponseMapper;
import com.dominator.gearly.ordering.application.AdminOrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <b>Every admin read endpoint S13 moved still answers where it did.</b>
 *
 * <p>This commit is the one in the sprint that relocates handlers rather than
 * implementations, and it makes three claims a reviewer cannot check by reading:
 *
 * <ol>
 *   <li>{@code /api/admin/orders/quantity-sold} and {@code /top5} moved from
 *       {@link AdminOrderController} to {@link AdminSalesController}, which is mapped at the
 *       <em>same</em> {@code /api/admin/orders} base. Two controllers sharing a base path is
 *       legal only while no full path collides — and if one did, Spring fails at startup with
 *       an ambiguous-mapping error, which no unit test would have caught.</li>
 *   <li>{@code /api/admin/dashboard/product-in-low-stock} moved to a controller in a different
 *       <em>context</em>, {@link AdminLowStockController}, and declares its path in full rather
 *       than through a class-level prefix.</li>
 *   <li>The four remaining dashboard panels are unaffected by
 *       {@code AdminDashboardService} being deleted from the middle.</li>
 * </ol>
 *
 * <p>A 200 is asserted rather than a body: the bodies are the query classes' business and are
 * covered by {@code SalesAnalyticsQueryIntegrationTest} against a real MongoDB. What is at
 * stake here is purely routing, so an empty result from a mocked query is the right stub —
 * a 404 would mean the URL moved.
 */
@WebMvcTest(controllers = {
        AdminDashboardController.class,
        AdminSalesController.class,
        AdminLowStockController.class,
        AdminOrderController.class})
@AutoConfigureMockMvc(addFilters = false)
@WithMockUser(roles = "ADMIN")
@DisplayName("the endpoints S13 moved keep their URLs")
class AnalyticsRoutesUnchangedTest {

    @Autowired private MockMvc mvc;

    @MockBean private DashboardProductQuery dashboardProductQuery;
    @MockBean private DashboardCustomerQuery dashboardCustomerQuery;
    @MockBean private SalesAnalyticsQuery salesAnalyticsQuery;
    @MockBean private ProductQueryService productQueryService;
    @MockBean private AdminOrderService adminOrderService;
    @MockBean private OrderResponseMapper orderResponseMapper;
    // Collaborators of the JwtAuthenticationFilter the slice picks up. Unused: addFilters=false
    // takes the chain out, because this test is about routing and not about authorization —
    // AdminOrderSecurityTest and AdminMethodSecurityTest own that question.
    @MockBean private com.dominator.gearly.identity.domain.AccessTokens accessTokens;
    @MockBean private com.dominator.gearly.identity.domain.UserRepository userRepository;

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
            // Sales reports — off AdminOrderController, onto AdminSalesController, same base path.
            "/api/admin/orders/quantity-sold",
            "/api/admin/orders/top5",
            // Dashboard panels — the pass-through service in the middle is gone.
            "/api/admin/dashboard/top-by-category",
            "/api/admin/dashboard/top-products",
            "/api/admin/dashboard/loyal-customers",
            "/api/admin/dashboard/top-user-by-avg-order-value",
            // Low stock — now answered by the catalog, at the URL it always had.
            "/api/admin/dashboard/product-in-low-stock",
            // Still AdminOrderController's, and proof the shared base path did not break it.
            "/api/admin/orders"})
    void stillAnswers(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isOk());
    }

    /**
     * The {@code ?period=} parameter survived the move. It binds to {@link TimeFrame}, which
     * moved package as well — from {@code model} into {@code analytics.api}.
     */
    @ParameterizedTest(name = "period={0}")
    @ValueSource(strings = {"ALL", "ONE_MONTH", "THREE_MONTHS", "SIX_MONTHS", "ONE_YEAR"})
    void timeFrameStillBinds(String period) throws Exception {
        mvc.perform(get("/api/admin/orders/quantity-sold").param("period", period))
                .andExpect(status().isOk());
    }
}
