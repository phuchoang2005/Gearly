package com.dominator.gearly.catalog.api;

import com.dominator.gearly.config.CorsConfig;
import com.dominator.gearly.platform.security.SecurityConfig;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.platform.security.JwtAuthenticationFilter;
import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.catalog.application.AdminProductService;
import com.dominator.gearly.catalog.application.ProductQueryService;
import com.dominator.gearly.catalog.domain.ProductNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 2 verification for an admin controller: the admin lock (403 for non-admins)
 * plus a ResourceNotFoundException surfacing as a uniform 404 ErrorResponse.
 */
@WebMvcTest(controllers = AdminProductController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class AdminProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AdminProductService adminProductService;
    @MockBean
    private ProductQueryService productQueryService;
    @MockBean
    private ProductSearchMapper productSearchMapper;

    // JwtAuthenticationFilter collaborators.
    @MockBean
    private AccessTokens accessTokens;
    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getProduct_asCustomer_returns403() throws Exception {
        mvc.perform(get("/api/admin/products/anything"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getProduct_anonymous_isRejected() throws Exception {
        mvc.perform(get("/api/admin/products/anything"))
                .andExpect(status().is4xxClientError()); // 401/403, never 200
    }

    /**
     * The use case used to answer {@code null} for a missing product and the controller turned
     * that into a 404 by hand. It throws {@code ProductNotFoundException} now — a
     * {@code DomainNotFoundException}, mapped centrally — so the 404 is produced by
     * {@code GlobalExceptionHandler} rather than by every caller remembering to check.
     */
    @Test
    @WithMockUser(roles = "ADMIN")
    void getProduct_asAdmin_notFound_returns404() throws Exception {
        when(adminProductService.getProductById(anyString()))
                .thenThrow(new ProductNotFoundException("Product not found"));

        mvc.perform(get("/api/admin/products/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Product not found"));
    }
}
