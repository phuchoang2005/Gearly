package com.dominator.gearly.identity.api;

import com.dominator.gearly.identity.domain.SignInRefusedException;
import com.dominator.gearly.platform.config.CorsConfig;

import com.dominator.gearly.identity.application.AuthService;
import com.dominator.gearly.identity.application.UserProfileService;
import com.dominator.gearly.identity.application.VerificationTokenService;
import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.UserNotFoundException;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.platform.security.JwtAuthenticationFilter;
import com.dominator.gearly.platform.security.SecurityConfig;
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
 * Sprint 2 verification, carried forward: GlobalExceptionHandler produces uniform ErrorResponse
 * bodies for a representative user controller. Web-layer slice (no MongoDB); the real
 * SecurityConfig keeps the exercised endpoints (login, forgot-password) public.
 */
@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class, UserResponseMapper.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class UserControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserProfileService userProfileService;
    @MockBean
    private AuthService authService;
    @MockBean
    private VerificationTokenService verificationTokenService;

    // JwtAuthenticationFilter collaborators (the filter is a no-op without a Bearer header).
    @MockBean
    private AccessTokens accessTokens;
    @MockBean
    private UserRepository users;

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
        when(authService.login(any(), any())).thenThrow(SignInRefusedException.invalidCredentials());

        mvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Invalid credentials"));
    }

    /**
     * Still a 404 with the same body — but raised as {@code UserNotFoundException}, a
     * shared-kernel {@code DomainNotFoundException}, rather than as the web layer's
     * {@code ResourceNotFoundException}. The domain says what is missing; the handler decides
     * what that is over HTTP.
     */
    @Test
    void forgotPassword_emailNotFound_returns404() throws Exception {
        doThrow(new UserNotFoundException("Email not registered."))
                .when(authService).handleForgotPassword(any());

        mvc.perform(post("/api/users/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Email not registered."));
    }
}
