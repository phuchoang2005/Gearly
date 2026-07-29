package com.dominator.bookify.controller.user;

import com.dominator.bookify.config.CorsConfig;
import com.dominator.bookify.config.SecurityConfig;
import com.dominator.bookify.exception.ResourceNotFoundException;
import com.dominator.bookify.exception.UnauthorizedException;
import com.dominator.bookify.repository.UserRepository;
import com.dominator.bookify.security.JwtAuthenticationFilter;
import com.dominator.bookify.security.JwtUtil;
import com.dominator.bookify.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sprint 2 verification: GlobalExceptionHandler produces uniform ErrorResponse bodies
 * for a representative user controller. Web-layer slice (no MongoDB); the real
 * SecurityConfig keeps the exercised endpoints (login, forgot-password) public.
 */
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    // JwtAuthenticationFilter collaborators (filter is a no-op without a Bearer header).
    @MockBean
    private JwtUtil jwtUtil;
    @MockBean
    private UserRepository userRepository;

    @Test
    void login_invalidBody_returns400_withFieldErrors() throws Exception {
        mvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        when(userService.login(any())).thenThrow(new UnauthorizedException("Invalid credentials"));

        mvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    @Test
    void forgotPassword_emailNotFound_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Email not registered."))
                .when(userService).handleForgotPassword(any());

        mvc.perform(post("/api/users/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Email not registered."));
    }
}
