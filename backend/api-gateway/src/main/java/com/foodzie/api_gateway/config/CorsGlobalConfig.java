package com.foodzie.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Global CORS configuration for the API Gateway.
 *
 * Spring Cloud Gateway (WebFlux) requires a reactive CorsWebFilter — not the
 * servlet-based WebMvcConfigurer. This filter runs before any route predicate
 * or AuthenticationFilter, so all origins are resolved correctly for both
 * preflight OPTIONS and actual requests.
 */
@Configuration
public class CorsGlobalConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow all frontend dev origins and production
        // Use patterns to support Vercel preview URLs like https://food-delievery-app-b6b3-*.vercel.app
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:4200",
                "https://food-delievery-app-b6b3.vercel.app",
                "https://food-delievery-app-b6b3-.*\\.vercel\\.app"  // Regex pattern for Vercel preview URLs
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsWebFilter(source);
    }
}
