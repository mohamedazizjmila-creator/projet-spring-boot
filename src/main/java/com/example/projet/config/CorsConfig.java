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
                            "http://10.0.2.2:*",           // Android Emulator - AJOUTÉ
                            "https://*.onrender.com",      // Frontend sur Render
                            "https://projet-api-v2.onrender.com",  // Backend
                            "https://*.netlify.app",       // Netlify
                            "https://697bf752fb63ccf804122fde--comforting-chimera-70cf77.netlify.app"  // Site Netlify
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
                
                // Configuration POUR LES IMAGES (uploads) - PERMET TOUT
                registry.addMapping("/uploads/**")
                        .allowedOriginPatterns("*") // PERMET TOUTES LES ORIGINES POUR LES IMAGES
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
                        
                // Configuration POUR LES IMAGES (images) - si vous avez aussi /images/
                registry.addMapping("/images/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}