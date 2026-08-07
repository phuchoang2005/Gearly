package com.dominator.gearly.platform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** OpenAPI metadata for the generated Swagger UI. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gearlyOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Gearly API")
                .description("Backend for the Gearly PC / computer-component store")
                .version("v1"));
    }
}
