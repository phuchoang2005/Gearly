package com.dominator.gearly.controller.admin;

import com.dominator.gearly.config.CorsConfig;
import com.dominator.gearly.config.SecurityConfig;
import com.dominator.gearly.repository.UserRepository;
import com.dominator.gearly.security.JwtAuthenticationFilter;
import com.dominator.gearly.security.JwtUtil;
import com.dominator.gearly.service.admin.AdminBookService;
import com.dominator.gearly.service.user.BookService;
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
@WebMvcTest(controllers = AdminBookController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class AdminBookControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AdminBookService adminBookService;
    @MockBean
    private BookService bookService;

    // JwtAuthenticationFilter collaborators.
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserRepository userRepository;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void getBook_asCustomer_returns403() throws Exception {
        mvc.perform(get("/api/admin/books/anything"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBook_anonymous_isRejected() throws Exception {
        mvc.perform(get("/api/admin/books/anything"))
                .andExpect(status().is4xxClientError()); // 401/403, never 200
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getBook_asAdmin_notFound_returns404() throws Exception {
        when(adminBookService.getBookById(anyString())).thenReturn(null);

        mvc.perform(get("/api/admin/books/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Book not found"));
    }
}
