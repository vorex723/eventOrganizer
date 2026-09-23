package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.jwt.JwtRequestFilter;
import com.mazurek.eventOrganizer.auth.ratelimit.AuthRateLimitFilter;
import com.mazurek.eventOrganizer.auth.ratelimit.AuthRateLimitStore;
import com.mazurek.eventOrganizer.auth.ratelimit.ClientAddressResolver;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtRequestFilter jwtRequestFilter;
    private final ObjectProvider<AuthRateLimitStore> authRateLimitStore;
    private final ObjectProvider<ClientAddressResolver> clientAddressResolver;
    private final ObjectProvider<AuthProperties> authProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        AuthRateLimitStore store = authRateLimitStore.getIfAvailable();
        ClientAddressResolver resolver = clientAddressResolver.getIfAvailable();
        AuthProperties properties = authProperties.getIfAvailable();
        if (store != null && resolver != null && properties != null) {
            http.addFilterBefore(
                    new AuthRateLimitFilter(properties, store, resolver),
                    SecurityContextHolderFilter.class
            );
        }
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
