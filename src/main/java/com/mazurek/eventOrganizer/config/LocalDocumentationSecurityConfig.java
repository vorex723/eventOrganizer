package com.mazurek.eventOrganizer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("local & !production")
public class LocalDocumentationSecurityConfig {

    @Bean
    @Order(1)
    SecurityFilterChain localDocumentationSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(SecurityConfig.DOCUMENTATION_PATHS)
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }
}
