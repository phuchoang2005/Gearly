package com.dominator.gearly.identity.infrastructure;

import com.dominator.gearly.identity.domain.VerificationTokenTtl;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Turns the identity context's configuration into the domain types the context is written in.
 * Same shape as {@code CatalogConfiguration}: the properties class is infrastructure's, the
 * value object it produces is the domain's, and nothing in the domain names Spring Boot.
 */
@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfiguration {

    @Bean
    public VerificationTokenTtl verificationTokenTtl(IdentityProperties properties) {
        return new VerificationTokenTtl(properties.getVerificationTokenTtl());
    }
}
