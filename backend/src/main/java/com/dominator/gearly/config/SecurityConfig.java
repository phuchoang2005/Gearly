package com.dominator.gearly.config;

import com.dominator.gearly.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
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
                    // Genuinely public routes: auth, catalog reads, reviews, guest cart,
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
                        "/api/reviews/**",
                        "/api/reviews",
                        "/api/reviews/distribution",
                        "/api/reviews/best-six",
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
                    // Everything else under /api/admin/** now requires the ADMIN role.
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
