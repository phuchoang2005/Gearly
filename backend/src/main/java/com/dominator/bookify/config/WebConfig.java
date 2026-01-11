package com.dominator.bookify.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve files under uploads/ when requested at /api/admin/uploads/**
        registry.addResourceHandler("/api/admin/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(3600);

        // (if you still have other static mappings, keep them here)
    }
}
