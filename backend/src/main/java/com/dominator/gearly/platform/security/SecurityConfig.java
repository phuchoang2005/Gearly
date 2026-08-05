package com.dominator.gearly.platform.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * The URL rules and the filter chain. Moved here from {@code config.SecurityConfig} in S12,
 * with the rest of the access boundary.
 *
 * <h2>{@code @EnableMethodSecurity}</h2>
 * Turns on {@code @PreAuthorize}, which the admin controllers now carry <em>alongside</em> the
 * {@code /api/admin/**} URL rule below rather than instead of it. Two independent checks of the
 * same thing is the point: the URL rule is a prefix match on a string, and every endpoint that
 * has ever escaped one did so by being mounted somewhere the pattern did not reach. An
 * annotation on the method cannot be escaped by moving the method.
 *
 * <h2>The review endpoints, and why the method matters</h2>
 * {@code /api/reviews/**} used to be {@code permitAll} as a single pattern — and it matches
 * {@code POST /api/reviews/submit-review} just as happily as the three public reads. Submitting
 * a review anonymously therefore reached the controller with a null principal, dereferenced it,
 * and returned a <b>500</b> where it should have said <b>401</b>. Worse than the status code:
 * the endpoint was never meant to be reachable without a session at all, and the only thing
 * stopping an anonymous caller from writing a review was that the code happened to crash first.
 *
 * <p>The reads are pinned to {@code GET} now, so the write falls through to
 * {@code anyRequest().authenticated()} like every other write in the application. The three
 * public review URLs are listed explicitly rather than as a wildcard, so a future
 * {@code POST /api/reviews/anything} cannot inherit public access by accident.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, CorsConfigurationSource corsConfigurationSource) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth ->
                auth
                    // Uploaded static assets (avatars, product media) are served under
                    // /uploads/** (spring.mvc.static-path-pattern) and must stay public so
                    // the storefront/admin can display them. The admin-only WRITE endpoint
                    // (POST /api/admin/media/upload) still falls under the /api/admin/** rule.
                    .requestMatchers("/uploads/**")
                    .permitAll()
                    // The public review reads. GET only, and enumerated — see the class note.
                    .requestMatchers(HttpMethod.GET,
                        "/api/reviews",
                        "/api/reviews/distribution",
                        "/api/reviews/best-six"
                    )
                    .permitAll()
                    // Genuinely public routes: auth, catalog reads, guest cart,
                    // payment webhooks, addresses, content pages, chat handshake.
                    .requestMatchers(
                        "/api/auth/google/**",
                        "/api/users/register",
                        "/api/users/login",
                        "/api/users/forgot-password",
                        "/api/users/reset-password",
                        "/api/users/verify",
                        "/api/users/resend-verification",
                        "/api/products/**",
                        "/api/categories",
                        "/api/guest-cart/**",
                        "/api/payments/**",
                        "/api/addresses/**",
                        "/api/blogposts/**",
                        "/api/pages/**",
                        "/ws-chat/**",
                        // OpenAPI / Swagger UI (API documentation)
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                    )
                    .permitAll()
                    // Everything else under /api/admin/** requires the ADMIN role. The
                    // controllers repeat this with @PreAuthorize; both apply.
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .anyRequest()
                    .authenticated()
            )
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .addFilterBefore(
                jwtAuthFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }
}
