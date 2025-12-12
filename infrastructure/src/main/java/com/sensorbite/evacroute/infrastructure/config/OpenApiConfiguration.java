package com.sensorbite.evacroute.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI evacuationRouteApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Evacuation Route Service API")
                        .description("Calculate safe evacuation routes avoiding flood zones")
                        .version("1.0.0"));
    }
}
