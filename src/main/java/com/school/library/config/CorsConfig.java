package com.school.library.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration =
                new CorsConfiguration();

        /*
         * ---------------------------------------------------------
         * Allowed Origins
         * ---------------------------------------------------------
         *
         * Example:
         * http://localhost:4200
         *
         * Production:
         * https://library.school.example
         */
        configuration.setAllowedOrigins(
                List.of(allowedOrigins.split(","))
        );

        /*
         * ---------------------------------------------------------
         * Allowed HTTP Methods
         * ---------------------------------------------------------
         */
        configuration.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        /*
         * ---------------------------------------------------------
         * Allowed Headers
         * ---------------------------------------------------------
         */
        configuration.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "X-Requested-With",
                        "X-CSRF-TOKEN"
                )
        );

        /*
         * ---------------------------------------------------------
         * Exposed Headers
         * ---------------------------------------------------------
         *
         * Headers that Angular is allowed to read.
         */
        configuration.setExposedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type"
                )
        );

        /*
         * ---------------------------------------------------------
         * Credentials
         * ---------------------------------------------------------
         *
         * Required when using HttpOnly refresh-token cookies.
         */
        configuration.setAllowCredentials(true);

        /*
         * ---------------------------------------------------------
         * Preflight Cache
         * ---------------------------------------------------------
         *
         * Browser can cache OPTIONS response for 1 hour.
         */
        configuration.setMaxAge(3600L);

        /*
         * ---------------------------------------------------------
         * Apply configuration to all endpoints
         * ---------------------------------------------------------
         */
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                configuration
        );

        return source;
    }
}

