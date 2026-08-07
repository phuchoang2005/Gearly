package com.dominator.gearly.platform.security;

import com.dominator.gearly.config.CorsConfig;
import com.dominator.gearly.platform.security.SecurityConfig;
import com.dominator.gearly.catalog.api.MediaController;
import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * S7 avatar/media serving: uploaded assets are served statically at /uploads/**
 * and must stay public, while the admin WRITE endpoint stays locked. Exercises
 * the real {@link SecurityConfig} filter chain (no MongoDB).
 */
@WebMvcTest(controllers = MediaController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
class UploadsSecurityTest {

    @Autowired
    private MockMvc mvc;

    // Collaborators of the real JwtAuthenticationFilter (unused without a Bearer header).
    @MockBean
    private AccessTokens accessTokens;
    @MockBean
    private UserRepository userRepository;
    // MediaController's collaborator. Mocked because this test is about the filter chain:
    // no request here ever reaches the handler.
    @MockBean
    private com.dominator.gearly.storage.domain.FileStorage fileStorage;

    @Test
    void uploadedAsset_isPublic() throws Exception {
        // Anonymous read of a (missing) avatar file: passing security yields 404,
        // not the 401 an authenticated-only path would produce.
        mvc.perform(get("/uploads/avatars/nobody.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void mediaUpload_requiresAdmin() throws Exception {
        // The write endpoint stays behind /api/admin/** -> ADMIN.
        mvc.perform(post("/api/admin/media/upload"))
                .andExpect(status().is4xxClientError()); // 401/403, never a successful upload
    }
}
