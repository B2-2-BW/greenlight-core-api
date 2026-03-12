package com.winten.greenlight.core.api.controller;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

// http://localhost:18080/swagger-ui/index.html
@Configuration
public class SwaggerConfig {
    @Value("${spring.profiles.active")
    private String activeProfile;

    @Value("${server.url}")
    private String serverUrl;

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI openAPI() {
        var openApi = new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .info(apiInfo());
        if ("local".equals(activeProfile)) {
            openApi.servers(List.of(new Server().url("http://localhost:"+serverPort).description("Greenlight Back Office Localhost API 서버")));
        } else {
            openApi.servers(List.of(new Server().url(serverUrl).description("Greenlight Back Office HTTPS API 서버")));
        }
        return openApi;
    }

    private Info apiInfo() {
        return new Info().title("Greenlight")
                .description("Greenlight Back Office REST API.")
                .version("1.0").contact(new Contact().name("Daniel Choi").email("danielchoi1115@gmail.com"))
                .license(new License().name("License of API")
                        .url("API license URL"));
    }
}