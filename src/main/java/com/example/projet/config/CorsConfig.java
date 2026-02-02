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
                // Configuration pour les API - AUTORISE TOUT POUR TESTS
                registry.addMapping("/api/**")
                        .allowedOriginPatterns(
                            "*", // AUTORISE TOUTES LES ORIGINES (y compris mobile)
                            "http://localhost:*",          // React en local
                            "http://127.0.0.1:*",          // React en local
                            "http://10.0.2.2:*",           // Android Emulator
                            "http://10.0.3.2:*",           // Genymotion
                            "http://192.168.*:*",          // Réseau local WiFi
                            "https://*.onrender.com",      // Frontend React sur Render
                            "https://projet-api-v2.onrender.com",  // Ton backend
                            "https://*.netlify.app",       // NETLIFY
                            "https://697bf752fb63ccf804122fde--comforting-chimera-70cf77.netlify.app"  // TON SITE NETLIFY
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
                
                // Configuration POUR LES IMAGES
                registry.addMapping("/uploads/**")
                        .allowedOriginPatterns("*") // AUTORISE TOUT POUR LES IMAGES
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600);
            }
        };
    }
}