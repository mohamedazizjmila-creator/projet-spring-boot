package com.example.projet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                // Configuration pour les API
                registry.addMapping("/api/**")
                        .allowedOriginPatterns(
                            "http://localhost:*",          // React en local
                            "http://127.0.0.1:*",          // React en local
                            "https://*.onrender.com",      // TON frontend React sur Render
                            "https://projet-api-v2.onrender.com"  // Ton backend actuel
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
                
                // Configuration POUR LES IMAGES (uploads)
                registry.addMapping("/uploads/**")
                        .allowedOriginPatterns(
                            "http://localhost:*",
                            "http://127.0.0.1:*",
                            "https://*.onrender.com"
                        )
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}