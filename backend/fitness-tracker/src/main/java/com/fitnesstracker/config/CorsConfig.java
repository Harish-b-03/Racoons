package com.fitnesstracker.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final AppConfig appConfig;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(appConfig.getCors().getAllowedOrigins()));
        configuration.setAllowedMethods(Arrays.asList(appConfig.getCors().getAllowedMethods()));
        configuration.setAllowedHeaders(Arrays.asList(appConfig.getCors().getAllowedHeaders()));
        configuration.setAllowCredentials(appConfig.getCors().getAllowCredentials());
        configuration.setMaxAge(appConfig.getCors().getMaxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
