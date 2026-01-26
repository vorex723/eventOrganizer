package com.mazurek.eventOrganizer.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;


import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtRequestFilterTest {

    private JwtRequestFilter jwtRequestFilter;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private final String authorizationHeader =
            "Bearer eyJhbGciOiJIUzI1NiJ9.eyJyb2xlcyI6WyJST0xFX1VTRVIiXSwidXNlcklkIjoiM2VlYzFiMjktN2Y5YS00ZmNiLWJhOWYtODBjNDU2MTc5ZDAyIiwic3ViIjoibm9ybWFsQGV2ZW50b3JnYW5pemVyLmNvbSIsImlhdCI6MTc2ODA2Mjc1OCwiZXhwIjoxNzY4MDY0NTU4fQ.Ht1M6feiLITrB6qsEb9w-TQ1nI9GGKUAMuvVUYGv3mc";

    private String token = authorizationHeader.substring(7);

    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken;

    private final String USER_EMAIL = "example@dot.com";
    private final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtRequestFilter = new JwtRequestFilter(jwtUtils);


        usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                new JwtUserDetails(USER_ID, USER_EMAIL, Collections.emptyList()),
                null,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("When filtering request should not set authentication in SecurityContextHolder if authorization header is not present")
    public void whenFilteringRequestShouldNotSetAuthenticationInSecurityContextHolderIfAuthorizationHeaderIsNotPresent() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(null);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Expected to not set authentication.");
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }
    @Test
    @DisplayName("When filtering request should not set authentication in SecurityContextHolder if authorization header do not start with Bearer prefix")
    public void whenFilteringRequestShouldNotSetAuthenticationInSecurityContextHolderIfAuthorizationHeaderDoNotStartWithBearerPrefix() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader.substring(7));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Expected to not set authentication.");
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("When filtering request should validate provided token")
    public void whenFilteringRequestShouldValidateProvidedToken() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);


        verify(jwtUtils, times(1)).isTokenValid(token);
    }

    @Test
    @DisplayName("When filtering request should continue filter chain even when exception occurs")
    public void whenFilteringRequestShouldContinueFilterChainEvenWhenExceptionOccurs() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenThrow(new RuntimeException("Token validation failed"));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }
    @Test
    @DisplayName("When filtering request should not set authentication if token is invalid")
    public void whenFilteringRequestShouldNotSetAuthenticationIfTokenIsInvalid() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(false);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Expected to not set authentication in SecurityContextHolder.");
        verify(jwtUtils, times(1)).isTokenValid(token);
        verify(jwtUtils, never()).extractUsername(anyString());
        verify(jwtUtils, never()).extractUserId(any());
        verify(jwtUtils, never()).extractAuthorities(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("When filtering request should extract user email from token using jwt utils")
    public void whenFilteringRequestShouldExtractUserEmailFromTokenUsingJwtUtils() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);


        verify(jwtUtils, times(1)).extractUsername(token);
    }
    @Test
    @DisplayName("When filtering request should not set authentication in SecurityContextHolder if token is missing user email")
    public void whenFilteringRequestShouldNotSetAuthenticationInSecurityContextHolderIfTokenIsMissingUserEmail() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(null);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Expected to not set authentication in SecurityContextHolder.");
    }

    @Test
    @DisplayName("When filtering request should not set authentication in SecurityContextHolder if SecurityContextHolder already contains authentication")
    public void whenFilteringRequestShouldNotSetAuthenticationInSecurityContextHolderIfSecurityContextHolderAlreadyContainsAuthentication() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(USER_EMAIL);

        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication(), "Expected to not set authentication in SecurityContextHolder.");
        assertEquals(usernamePasswordAuthenticationToken, SecurityContextHolder.getContext().getAuthentication(), "Expected to not change authentication in SecurityContextHolder.");
    }

    @Test
    @DisplayName("When filtering request should extract user id and user roles from token for UserDetails")
    public void whenFilteringRequestShouldExtractUserIdAndUserRolesFromTokenForUserDetails() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(USER_EMAIL);
        when(jwtUtils.extractUserId(token)).thenReturn(USER_ID);
        when(jwtUtils.extractAuthorities(anyString())).thenAnswer(inv ->
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtils, times(1)).extractUserId(token);
        verify(jwtUtils, times(1)).extractAuthorities(token);

    }

    @Test
    @DisplayName("When filtering request should set correct authentication in SecurityContextHolder")
    public void whenFilteringRequestShouldSetCorrectAuthenticationInSecurityContextHolder() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(USER_EMAIL);
        when(jwtUtils.extractUserId(token)).thenReturn(USER_ID);
        when(jwtUtils.extractAuthorities(anyString())).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertTrue(authentication.isAuthenticated(), "Expected user to be authenticated.");
        assertEquals(1, authentication.getAuthorities().size(), "Expected one authority.");
        assertTrue(authentication.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_USER")),
                "Expected ROLE_USER authority.");
        assertInstanceOf(JwtUserDetails.class, authentication.getPrincipal(), "Expected to set principal as JwtUserDetails object.");
        JwtUserDetails principal = (JwtUserDetails) authentication.getPrincipal();
        assertEquals(USER_ID, principal.getId(), "Expected to set correct user id.");
        assertEquals(USER_EMAIL, principal.getEmail(), "Expected to set correct email.");
        assertEquals(1, principal.getAuthorities().size(), "Expected one authority on principal.");
        assertTrue(principal.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("ROLE_USER")),
                "Expected ROLE_USER authority on principal.");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("When filtering request should set correct authentication if user has more than one role in SecurityContextHolder")
    public void whenFilteringRequestShouldSetCorrectAuthenticationIfUserHasMoreThanOneRoleInSecurityContextHolder() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(USER_EMAIL);
        when(jwtUtils.extractUserId(token)).thenReturn(USER_ID);
        when(jwtUtils.extractAuthorities(anyString())).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));

        List<String> roleNames = List.of("ROLE_USER", "ROLE_ADMIN");

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertTrue(authentication.isAuthenticated(), "Expected user to be authenticated.");
        assertEquals(2, authentication.getAuthorities().size(), "Expected two authorities.");
        assertTrue(authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .allMatch(roleNames::contains),
                "Expected both ROLE_USER and ROLE_ADMIN authorities on authentication token.");
        assertInstanceOf(JwtUserDetails.class, authentication.getPrincipal(), "Expected to set principal as JwtUserDetails object.");
        JwtUserDetails principal = (JwtUserDetails) authentication.getPrincipal();
        assertEquals(USER_ID, principal.getId(), "Expected to set correct user id.");
        assertEquals(USER_EMAIL, principal.getEmail(), "Expected to set correct email.");
        assertEquals(2, principal.getAuthorities().size(), "Expected two authorities on principal.");
        assertTrue(principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .allMatch(roleNames::contains),
                "Expected both ROLE_USER and ROLE_ADMIN authorities on principal.");
        verify(filterChain, times(1)).doFilter(request, response);
    }

  }