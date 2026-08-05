package com.dominator.gearly.cart.api;

import com.dominator.gearly.cart.application.CartService;
import com.dominator.gearly.cart.domain.CartFixture;
import com.dominator.gearly.cart.domain.GuestCartIds;
import com.dominator.gearly.config.CorsConfig;
import com.dominator.gearly.exception.GlobalExceptionHandler;
import com.dominator.gearly.identity.domain.AccessTokens;
import com.dominator.gearly.identity.domain.UserRepository;
import com.dominator.gearly.platform.security.HmacGuestCartIds;
import com.dominator.gearly.platform.security.JwtAuthenticationFilter;
import com.dominator.gearly.platform.security.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <b>The fourth S12 security item: a guest cart id is bound to this server.</b>
 *
 * <p>{@code /api/guest-cart/**} is {@code permitAll} and always will be — a guest has no account
 * to authenticate. What was missing is the weaker property that the id be unforgeable: any
 * string at all was accepted, so learning a UUID meant owning that basket, and a loop over
 * arbitrary strings created a document per attempt through {@code getOrCreate}.
 *
 * <p>Asserted through the real chain with the <b>real</b> {@link HmacGuestCartIds}, because a
 * mocked port would prove only that the controller calls something. {@link CartService} is
 * mocked, which is what makes the second assertion in each test meaningful: on a refusal it must
 * never be reached, or the spray-creation half of the hole is still open.
 */
@WebMvcTest(controllers = GuestCartController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationFilter.class,
        GlobalExceptionHandler.class, CartResponseMapper.class,
        GuestCartBindingTest.RealSigning.class})
@TestPropertySource(properties = "cors.allowed-origins=http://localhost:5173")
@DisplayName("only an id this server signed reaches the cart")
class GuestCartBindingTest {

    @TestConfiguration
    static class RealSigning {
        @Bean
        GuestCartIds guestCartIds() {
            return new HmacGuestCartIds("test-jwt-secret-key-that-is-long-enough-32bytes");
        }
    }

    @Autowired private MockMvc mvc;
    @Autowired private GuestCartIds guestCartIds;
    @Autowired private ObjectMapper json;

    @MockBean private CartService cartService;

    // Collaborators of the real JwtAuthenticationFilter (unused without a Bearer header).
    @MockBean private AccessTokens accessTokens;
    @MockBean private UserRepository userRepository;

    @Test
    @DisplayName("init hands out a signed id and creates the basket behind the raw UUID")
    void initIssuesASignedId() throws Exception {
        when(cartService.getOrCreate(isNull(), any())).thenReturn(CartFixture.aCart().forGuest("g1").build());

        String body = mvc.perform(post("/api/guest-cart/init"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String issued = json.readTree(body).get("guestId").asText();
        String raw = guestCartIds.verify(issued).orElseThrow();

        // The wire format changed; the stored key did not — carts.guestId is still the UUID.
        assertThatIsAUuid(raw);
        verify(cartService).getOrCreate(isNull(), eq(raw));
    }

    @Test
    @DisplayName("a signed id is accepted")
    void aSignedIdReadsTheCart() throws Exception {
        String issued = guestCartIds.issue();
        String raw = guestCartIds.verify(issued).orElseThrow();
        when(cartService.getOrCreate(isNull(), eq(raw))).thenReturn(CartFixture.aCart().forGuest("g1").build());

        mvc.perform(get("/api/guest-cart").param("guestId", issued))
                .andExpect(status().isOk());
    }

    /**
     * The hole, stated as a test: a bare UUID is exactly what an attacker who read one out of an
     * access log or a referrer header would present, and it is what every guest client sent
     * before S12.
     */
    @Test
    @DisplayName("a bare UUID is 403 and never reaches the service")
    void anUnsignedIdIsRefused() throws Exception {
        mvc.perform(get("/api/guest-cart").param("guestId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(cartService, never()).getOrCreate(any(), any());
    }

    /**
     * The other half: {@code getOrCreate} inserts on miss, so an unsigned id reaching it wrote a
     * document. Refusing before the service is what stops arbitrary strings creating carts.
     */
    @Test
    @DisplayName("an arbitrary string creates no cart")
    void anArbitraryStringCreatesNothing() throws Exception {
        mvc.perform(get("/api/guest-cart").param("guestId", "../../etc/passwd"))
                .andExpect(status().isForbidden());

        verify(cartService, never()).getOrCreate(any(), any());
    }

    @Test
    @DisplayName("every mutating route is bound too, not just the read")
    void theMutationsAreBoundAsWell() throws Exception {
        String bare = UUID.randomUUID().toString();

        mvc.perform(post("/api/guest-cart/add").param("guestId", bare)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"p1\",\"quantity\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/guest-cart/clear").param("guestId", bare))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/guest-cart/remove")
                        .param("guestId", bare).param("productId", "p1"))
                .andExpect(status().isForbidden());

        verify(cartService, never()).addItem(any(), any(), any(), any());
        verify(cartService, never()).clearCart(any(), any());
        verify(cartService, never()).removeItem(any(), any(), any());
    }

    private static void assertThatIsAUuid(String raw) {
        UUID.fromString(raw);   // throws if it is not one
    }
}
