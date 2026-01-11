package com.dominator.bookify.config;

import com.dominator.bookify.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors()
            .and()
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth ->
                auth
                    .requestMatchers(
                        "/api/auth/google/**",
                        "/api/users/register",
                        "/api/users/login",
                        "/api/users/forgot-password",
                        "/api/users/reset-password",
                        "/api/users/verify",
                        "/api/users/resend-verification",
                        "/api/books/**",
                        "/api/categories",
                        "/api/reviews/**",
                        "/api/admin/books/**",
                        "/api/admin/users/**",
                        "/api/admin/orders/**",
                        "/api/admin/categories/**",
                        "/api/reviews",
                        "/api/reviews/distribution",
                        "/api/reviews/best-six",
                        "/api/guest-cart/**",
                        "/api/payments/**",
                        "/api/addresses/**",
                        "/api/blogposts/**",
                        "/api/pages/**",
                        "/api/admin/media/**",
                        "/api/admin/**",
                        "/api/admin/uploads/**",
                        "/ws-chat/**"
                    )
                    .permitAll()
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
