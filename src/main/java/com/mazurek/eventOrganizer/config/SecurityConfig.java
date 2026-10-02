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
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    static final String LOCAL_AUTH_EMAILS_PATH = "/api/v1/dev/auth-emails";
    static final String[] DOCUMENTATION_PATHS = {
            "/v3/api-docs",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/swagger-ui.html",
            "/swagger-ui/**"
    };

    private final JwtRequestFilter jwtRequestFilter;
    private final ObjectProvider<AuthRateLimitStore> authRateLimitStore;
    private final ObjectProvider<ClientAddressResolver> clientAddressResolver;
    private final ObjectProvider<AuthProperties> authProperties;
    private final ApiAuthenticationEntryPoint apiAuthenticationEntryPoint;
    private final ApiAccessDeniedHandler apiAccessDeniedHandler;
    private final ApiErrorResponseWriter apiErrorResponseWriter;

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(apiAuthenticationEntryPoint)
                        .accessDeniedHandler(apiAccessDeniedHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.HEAD, "/actuator/health").permitAll()
                        .requestMatchers("/actuator", "/actuator/**").denyAll()
                        .requestMatchers("/api/v1/dev", "/api/v1/dev/**").denyAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(DOCUMENTATION_PATHS).denyAll()
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/events",
                                "/api/v1/events/{eventId}",
                                "/api/v1/cities/search",
                                "/api/v1/cities/{cityId}",
                                "/api/v1/cities/{cityId}/events",
                                "/api/v1/tags/{tagName}",
                                "/api/v1/tags/{tagName}/events"
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
            http.addFilterAfter(
                    new AuthRateLimitFilter(properties, store, resolver, apiErrorResponseWriter),
                    CorsFilter.class
            );
        }
        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
