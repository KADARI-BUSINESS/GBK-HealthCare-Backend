package com.gbkhealthcare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String[] allowedOrigins;

    public WebConfig(@Value("${app.security.allowed-origins:http://localhost:5173,https://main.d2jr9663g6l11x.amplifyapp.com}") String allowedOrigins) {
        this.allowedOrigins = allowedOrigins.split(",");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowCredentials(true)
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "DELETE",
                        "OPTIONS")
                .allowedHeaders("Authorization", "Content-Type", "Accept", "X-XSRF-TOKEN")
                .maxAge(3600);
    }
}
