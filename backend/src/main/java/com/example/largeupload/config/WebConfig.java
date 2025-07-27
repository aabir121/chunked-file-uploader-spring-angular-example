package com.example.largeupload.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class WebConfig implements WebFluxConfigurer {

    private final FileUploadProperties properties;

    public WebConfig(FileUploadProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        FileUploadProperties.Cors corsConfig = properties.getCors();

        registry.addMapping("/**")
                .allowedOrigins(corsConfig.getAllowedOrigins().toArray(new String[0]))
                .allowedMethods(corsConfig.getAllowedMethods().toArray(new String[0]))
                .allowedHeaders(corsConfig.getAllowedHeaders().toArray(new String[0]))
                .allowCredentials(corsConfig.isAllowCredentials())
                .maxAge(corsConfig.getMaxAge());
    }
}