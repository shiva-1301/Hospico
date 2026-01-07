package com.hospitalfinder.backend.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hospicoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hospico API")
                        .description("API documentation for Hospico Hospital Finder application")
                        .version("1.0"))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development Server"),
                        new Server().url("https://hospico-backend.onrender.com").description("Render Production"),
                        new Server().url("https://hospico-backend-50037434927.development.catalystappsail.in").description("AppSail Production")));
    }
}
