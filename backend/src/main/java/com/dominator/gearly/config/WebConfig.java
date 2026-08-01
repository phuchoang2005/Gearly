package com.dominator.gearly.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // CORS is configured centrally in CorsConfig / SecurityConfig — not here.

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files under uploads/ when requested at /api/admin/uploads/**
        registry.addResourceHandler("/api/admin/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600);
    }
}
