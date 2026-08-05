package com.dominator.gearly.platform.security;

import com.dominator.gearly.catalog.api.CategoryController;
import com.dominator.gearly.catalog.application.CategoryQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * <b>Proof that the {@code @PreAuthorize} annotations are not inert.</b>
 *
 * <p>S12 put {@code @PreAuthorize("hasRole('ADMIN')")} on every admin controller as defence in
 * depth behind {@code SecurityConfig}'s {@code /api/admin/**} URL rule. That creates a testing
 * trap: with the real chain in place <em>both</em> guards refuse a customer, so
 * {@code AdminOrderSecurityTest} would stay green with every annotation deleted. It would be
 * measuring the URL rule and reporting it as the annotation — exactly the inert-rule failure
 * S11 found in its own ArchUnit pass.
 *
 * <p>So this slice deliberately replaces the filter chain with one that <b>permits everything</b>
 * and keeps only {@link EnableMethodSecurity}. Anything refused here is refused by the
 * annotation and by nothing else. Falsified by removing the annotation from
 * {@code CategoryController.findAllForAdmin}: {@link #aCustomerIsRefusedByTheAnnotationAlone}
 * then returns 200.
 *
 * <p><b>What it caught on its first run.</b> The customer was refused, but with a <b>500</b>:
 * method security throws inside the dispatcher, so the refusal passed through
 * {@code GlobalExceptionHandler}, whose {@code @ExceptionHandler(Exception.class)} catch-all
 * swallowed it before {@code ExceptionTranslationFilter} could turn it into a 403. Every other
 * admin test was green because the URL rule refuses those requests in the filter chain, before
 * any handler runs. {@code GlobalExceptionHandler.rethrowAccessDenied} is the fix.
 *
 * <p>{@link CategoryController} is the subject because it is the one admin route that
 * <em>needs</em> the method-level guard rather than merely repeating the URL rule: the class
 * also serves the storefront's public {@code /api/categories}, so it cannot carry a class-level
 * annotation, and the two routes' different answers are the behaviour worth pinning.
 */
@WebMvcTest(controllers = CategoryController.class)
@Import(AdminMethodSecurityTest.PermitEverythingButKeepMethodSecurity.class)
@DisplayName("@PreAuthorize refuses a non-admin with no URL rule helping it")
class AdminMethodSecurityTest {

    /**
     * A filter chain with no authorization rules at all. Method security stays on, so the only
     * thing that can produce a 403 below is the annotation on the handler.
     */
    @TestConfiguration
    @EnableMethodSecurity
    static class PermitEverythingButKeepMethodSecurity {
        @Bean
        SecurityFilterChain permitAll(HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CategoryQueryService categoryQueryService;

    // The JWT filter is a component, so the slice instantiates it; its collaborators are unused
    // here because no request below carries a Bearer header.
    @MockBean private com.dominator.gearly.identity.domain.AccessTokens accessTokens;
    @MockBean private com.dominator.gearly.identity.domain.UserRepository userRepository;

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void aCustomerIsRefusedByTheAnnotationAlone() throws Exception {
        mvc.perform(get("/api/admin/categories")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anAdminIsAdmitted() throws Exception {
        when(categoryQueryService.getCategoriesWithProductCount()).thenReturn(List.of());

        mvc.perform(get("/api/admin/categories")).andExpect(status().isOk());
    }

    /**
     * The other half of the split: the storefront's category menu must stay reachable without
     * signing in. This is what a class-level annotation would have broken.
     */
    @Test
    void thePublicCategoryRouteIsStillAnonymous() throws Exception {
        when(categoryQueryService.getCategoriesWithProductCount()).thenReturn(List.of());

        mvc.perform(get("/api/categories")).andExpect(status().isOk());
    }
}
