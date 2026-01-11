package com.dominator.bookify.config;

import com.google.auth.oauth2.TokenVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GoogleAuthConfig {

    @Bean
    TokenVerifier googleTokenVerifier(@Value("${security.google.client-id}") String clientId) {

        return TokenVerifier.newBuilder()
                .setAudience(clientId)
                .setIssuer("https://accounts.google.com")
                .build();
    }
}
