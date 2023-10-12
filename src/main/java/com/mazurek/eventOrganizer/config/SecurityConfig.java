package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.jwt.JwtRequestFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtRequestFilter jwtRequestFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity
                .authorizeHttpRequests(auth -> auth.
                        requestMatchers( AntPathRequestMatcher.antMatcher("/h2-console/**")).permitAll())
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(AntPathRequestMatcher.antMatcher("/h2-console/**")));;

        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize ->
                        authorize.requestMatchers(AntPathRequestMatcher.antMatcher("/api/v1/auth/**")).permitAll()
                                .anyRequest().authenticated())
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);


        return httpSecurity.build();
    }

}
