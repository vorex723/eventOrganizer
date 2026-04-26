package com.mazurek.eventOrganizer.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.SoftAssertions;
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

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtRequestFilter unit tests:")
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

    private final String authorizationHeader = AuthConstants.JWT_PREFIX + JwtConstants.ACCESS_TOKEN;

    private String token = JwtConstants.ACCESS_TOKEN;

    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtRequestFilter = new JwtRequestFilter(jwtUtils);

        usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                new JwtUserDetails(UserConstants.FIRST_USER_ID, UserConstants.FIRST_USER_EMAIL, Collections.emptyList()),
                null,
                Collections.emptyList()
        );
    }

    @Test
    @DisplayName("When filtering request should not set authentication in SecurityContextHolder if authorization header is not present")
    public void whenFilteringRequestShouldNotSetAuthenticationInSecurityContextHolderIfAuthorizationHeaderIsNotPresent() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(null);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).as("Expected to not set authentication.").isNull();
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }
    @Test
    @DisplayName("When filtering request should not set authentication in SecurityContextHolder if authorization header do not start with Bearer prefix")
    public void whenFilteringRequestShouldNotSetAuthenticationInSecurityContextHolderIfAuthorizationHeaderDoNotStartWithBearerPrefix() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader.substring(7));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).as("Expected to not set authentication.").isNull();
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("When filtering request should validate provided token")
    public void whenFilteringRequestShouldValidateProvidedToken() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);


        verify(jwtUtils, times(1)).isTokenValid(token);
    }

    @Test
    @DisplayName("When filtering request should continue filter chain even when exception occurs")
    public void whenFilteringRequestShouldContinueFilterChainEvenWhenExceptionOccurs() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenThrow(new IllegalStateException("Token validation failed"));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain, times(1)).doFilter(request, response);
    }
    @Test
    @DisplayName("When filtering request should not set authentication if token is invalid")
    public void whenFilteringRequestShouldNotSetAuthenticationIfTokenIsInvalid() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(false);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("Expected to not set authentication in SecurityContextHolder.")
                .isNull();
        verify(jwtUtils, times(1)).isTokenValid(token);
        verify(jwtUtils, never()).extractUsername(anyString());
        verify(jwtUtils, never()).extractUserId(any());
        verify(jwtUtils, never()).extractAuthorities(anyString());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("When filtering request should extract user email from token using jwt utils")
    public void whenFilteringRequestShouldExtractUserEmailFromTokenUsingJwtUtils() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);


        verify(jwtUtils, times(1)).extractUsername(token);
    }
    @Test
    @DisplayName("When filtering request should not set authentication in SecurityContextHolder if token is missing user email")
    public void whenFilteringRequestShouldNotSetAuthenticationInSecurityContextHolderIfTokenIsMissingUserEmail() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(null);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("Expected to not set authentication in SecurityContextHolder.")
                .isNull();
    }

    @Test
    @DisplayName("When filtering request should not set authentication in SecurityContextHolder if SecurityContextHolder already contains authentication")
    public void whenFilteringRequestShouldNotSetAuthenticationInSecurityContextHolderIfSecurityContextHolderAlreadyContainsAuthentication() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(UserConstants.FIRST_USER_EMAIL);

        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication())
                .as("Expected to not set authentication in SecurityContextHolder.")
                .isNotNull()
                .isEqualTo(usernamePasswordAuthenticationToken);
    }

    @Test
    @DisplayName("When filtering request should extract user id and user roles from token for UserDetails")
    public void whenFilteringRequestShouldExtractUserIdAndUserRolesFromTokenForUserDetails() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(UserConstants.FIRST_USER_EMAIL);
        when(jwtUtils.extractUserId(token)).thenReturn(UserConstants.FIRST_USER_ID);
        when(jwtUtils.extractAuthorities(anyString())).thenAnswer(inv ->
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        verify(jwtUtils, times(1)).extractUserId(token);
        verify(jwtUtils, times(1)).extractAuthorities(token);

    }

    @Test
    @DisplayName("When filtering request should set correct authentication in SecurityContextHolder")
    public void whenFilteringRequestShouldSetCorrectAuthenticationInSecurityContextHolder() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(UserConstants.FIRST_USER_EMAIL);
        when(jwtUtils.extractUserId(token)).thenReturn(UserConstants.FIRST_USER_ID);
        when(jwtUtils.extractAuthorities(anyString())).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(authentication.isAuthenticated()).as("Expected user to be authenticated.").isTrue();
            softly.assertThat(authentication.getAuthorities()).as("Expected one authority.").hasSize(1);
            softly.assertThat(authentication.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
            softly.assertThat(authentication.getPrincipal())
                    .as("Expected to set principal as JwtUserDetails object.")
                    .isInstanceOf(JwtUserDetails.class);
        });
        JwtUserDetails principal = (JwtUserDetails) authentication.getPrincipal();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(principal.getId()).as("Expected to set correct user id.").isEqualTo(UserConstants.FIRST_USER_ID);
            softly.assertThat(principal.getEmail()).as("Expected to set correct email.").isEqualTo(UserConstants.FIRST_USER_EMAIL);
            softly.assertThat(principal.getAuthorities()).as("Expected one authority on principal.").hasSize(1);
            softly.assertThat(principal.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        });
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("When filtering request should set correct authentication if user has more than one role in SecurityContextHolder")
    public void whenFilteringRequestShouldSetCorrectAuthenticationIfUserHasMoreThanOneRoleInSecurityContextHolder() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(UserConstants.FIRST_USER_EMAIL);
        when(jwtUtils.extractUserId(token)).thenReturn(UserConstants.FIRST_USER_ID);
        when(jwtUtils.extractAuthorities(anyString())).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));

        List<String> roleNames = List.of("ROLE_USER", "ROLE_ADMIN");

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(authentication.isAuthenticated()).as("Expected user to be authenticated.").isTrue();
            softly.assertThat(authentication.getAuthorities()).as("Expected two authorities.").hasSize(2);
            softly.assertThat(authentication.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrderElementsOf(roleNames);
            softly.assertThat(authentication.getPrincipal())
                    .as("Expected to set principal as JwtUserDetails object.")
                    .isInstanceOf(JwtUserDetails.class);
        });
        JwtUserDetails principal = (JwtUserDetails) authentication.getPrincipal();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(principal.getId()).as("Expected to set correct user id.").isEqualTo(UserConstants.FIRST_USER_ID);
            softly.assertThat(principal.getEmail()).as("Expected to set correct email.").isEqualTo(UserConstants.FIRST_USER_EMAIL);
            softly.assertThat(principal.getAuthorities()).as("Expected two authorities on principal.").hasSize(2);
            softly.assertThat(principal.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrderElementsOf(roleNames);
        });
        verify(filterChain, times(1)).doFilter(request, response);
    }

  }
