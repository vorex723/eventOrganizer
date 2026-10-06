package com.mazurek.eventOrganizer.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.config.ApiAuthenticationEntryPoint;

import java.io.IOException;
import java.util.Collection;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return (HttpMethod.GET.matches(request.getMethod()) || HttpMethod.HEAD.matches(request.getMethod()))
                && request.getRequestURI().equals(request.getContextPath() + "/actuator/health");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")){
            filterChain.doFilter(request, response);
            return;
        }

        try{
            String token = authorizationHeader.substring(7);

            if (jwtUtils.isTokenValid(token)){
                String userEmail = jwtUtils.extractUsername(token);
                if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null){

                        UUID userId  = jwtUtils.extractUserId(token);
                        User user = userRepository.findById(userId).orElse(null);
                        if (user == null || !user.isActivated() || user.isBanned()
                                || user.getSecurityVersion() != jwtUtils.extractSecurityVersion(token)) {
                            filterChain.doFilter(request, response);
                            return;
                        }

                        Collection<? extends GrantedAuthority> authorities = jwtUtils.extractAuthorities(token);

                        JwtUserDetails userDetails = new JwtUserDetails(userId, userEmail, authorities);

                        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                authorities
                        );

                        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    }
            } else {
                request.setAttribute(ApiAuthenticationEntryPoint.INVALID_ACCESS_TOKEN_ATTRIBUTE, Boolean.TRUE);
            }
        } catch (Exception e){
            log.debug("Rejected invalid access token: failureType={}", e.getClass().getSimpleName());
            request.setAttribute(ApiAuthenticationEntryPoint.INVALID_ACCESS_TOKEN_ATTRIBUTE, Boolean.TRUE);
        }
        filterChain.doFilter(request, response);

    }
}
