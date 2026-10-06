package com.mazurek.eventOrganizer.jwt;

import com.mazurek.eventOrganizer.config.ApiAuthenticationEntryPoint;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
import com.mazurek.eventOrganizer.testData.builders.JwtUserDetailsTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtRequestFilter unit tests:")
class JwtRequestFilterUnitTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private JwtRequestFilter jwtRequestFilter;

    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserRepository userRepository;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private final String authorizationHeader = AuthConstants.JWT_PREFIX + JwtConstants.ACCESS_TOKEN;

    private final String token = JwtConstants.ACCESS_TOKEN;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtRequestFilter = new JwtRequestFilter(jwtUtils, userRepository);
    }

    private void setupActiveUserLookup() {
        User activeUser = UserTestBuilder.firstUser().build();
        when(userRepository.findById(activeUser.getId())).thenReturn(Optional.of(activeUser));
        when(jwtUtils.extractSecurityVersion(token)).thenReturn(activeUser.getSecurityVersion());
    }

    @ParameterizedTest(name = "method={0}, contextPath={1}")
    @CsvSource({"GET, ''", "HEAD, ''", "GET, /backend", "HEAD, /backend"})
    @DisplayName("When reading health should bypass jwt validation and user lookup")
    void whenReadingHealthShouldBypassJwtValidationAndUserLookup(String method, String contextPath) throws ServletException, IOException {
        MockHttpServletRequest healthRequest = new MockHttpServletRequest(method, contextPath + "/actuator/health");
        healthRequest.setContextPath(contextPath);
        healthRequest.addHeader("Authorization", authorizationHeader);
        MockHttpServletResponse healthResponse = new MockHttpServletResponse();

        jwtRequestFilter.doFilter(healthRequest, healthResponse, filterChain);

        verify(filterChain).doFilter(healthRequest, healthResponse);
        verifyNoInteractions(jwtUtils, userRepository);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @ParameterizedTest(name = "method={0}, path={1}")
    @CsvSource({
            "POST, /actuator/health",
            "GET, /actuator/health/db",
            "GET, /actuator/health/",
            "GET, /api/v1/users/me"
    })
    @DisplayName("When request is not an exact health read should validate jwt")
    void whenRequestIsNotAnExactHealthReadShouldValidateJwt(String method, String path) throws ServletException, IOException {
        MockHttpServletRequest otherRequest = new MockHttpServletRequest(method, path);
        otherRequest.addHeader("Authorization", authorizationHeader);
        MockHttpServletResponse otherResponse = new MockHttpServletResponse();

        jwtRequestFilter.doFilter(otherRequest, otherResponse, filterChain);

        verify(jwtUtils).isTokenValid(token);
        verify(filterChain).doFilter(otherRequest, otherResponse);
    }

    @Test
    @DisplayName("When filtering request should not set authentication in SecurityContextHolder if authorization header is not present")
    public void whenFilteringRequestShouldNotSetAuthenticationInSecurityContextHolderIfAuthorizationHeaderIsNotPresent() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(null);

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).as("Expected to not set authentication.").isNull();
        verify(filterChain, times(1)).doFilter(request, response);
        verify(request, never()).setAttribute(eq(ApiAuthenticationEntryPoint.INVALID_ACCESS_TOKEN_ATTRIBUTE), any());
        verifyNoInteractions(jwtUtils, userRepository);
    }
    @Test
    @DisplayName("When filtering request should not set authentication in SecurityContextHolder if authorization header do not start with Bearer prefix")
    public void whenFilteringRequestShouldNotSetAuthenticationInSecurityContextHolderIfAuthorizationHeaderDoNotStartWithBearerPrefix() throws ServletException, IOException {
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader.substring(7));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).as("Expected to not set authentication.").isNull();
        verify(filterChain, times(1)).doFilter(request, response);
        verify(request, never()).setAttribute(eq(ApiAuthenticationEntryPoint.INVALID_ACCESS_TOKEN_ATTRIBUTE), any());
        verifyNoInteractions(jwtUtils, userRepository);
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
        verify(request).setAttribute(ApiAuthenticationEntryPoint.INVALID_ACCESS_TOKEN_ATTRIBUTE, Boolean.TRUE);
        verifyNoInteractions(userRepository);
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
        verify(request).setAttribute(ApiAuthenticationEntryPoint.INVALID_ACCESS_TOKEN_ATTRIBUTE, Boolean.TRUE);
        verifyNoInteractions(userRepository);
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
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                new JwtUserDetailsTestBuilder()
                        .id(UserConstants.FIRST_USER_ID)
                        .email(UserConstants.FIRST_USER_EMAIL)
                        .authorities(Collections.emptyList())
                        .build(),
                null,
                Collections.emptyList()
        );
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
        setupActiveUserLookup();
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
        setupActiveUserLookup();
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(UserConstants.FIRST_USER_EMAIL);
        when(jwtUtils.extractUserId(token)).thenReturn(UserConstants.FIRST_USER_ID);
        when(jwtUtils.extractAuthorities(anyString())).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_USER")));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);


        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).as("Expected authentication for an active user with a valid token").isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(JwtUserDetails.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(authentication.isAuthenticated()).as("Expected user to be authenticated.").isTrue();
            softly.assertThat(authentication.getAuthorities()).as("Expected one authority.").hasSize(1);
            softly.assertThat(authentication.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
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
        verify(request, never()).setAttribute(eq(ApiAuthenticationEntryPoint.INVALID_ACCESS_TOKEN_ATTRIBUTE), any());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("When filtering request should set correct authentication if user has more than one role in SecurityContextHolder")
    public void whenFilteringRequestShouldSetCorrectAuthenticationIfUserHasMoreThanOneRoleInSecurityContextHolder() throws ServletException, IOException {
        setupActiveUserLookup();
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(UserConstants.FIRST_USER_EMAIL);
        when(jwtUtils.extractUserId(token)).thenReturn(UserConstants.FIRST_USER_ID);
        when(jwtUtils.extractAuthorities(anyString())).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN")));

        List<String> roleNames = List.of("ROLE_USER", "ROLE_ADMIN");

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        assertThat(authentication).as("Expected authentication for an active user with multiple roles").isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(JwtUserDetails.class);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(authentication.isAuthenticated()).as("Expected user to be authenticated.").isTrue();
            softly.assertThat(authentication.getAuthorities()).as("Expected two authorities.").hasSize(2);
            softly.assertThat(authentication.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrderElementsOf(roleNames);
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

    @Test
    @DisplayName("When token security version differs from the current account version should not authenticate")
    void whenSecurityVersionDiffersShouldNotAuthenticate() throws ServletException, IOException {
        User user = UserTestBuilder.firstUser().securityVersion(2L).build();
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(UserConstants.FIRST_USER_EMAIL);
        when(jwtUtils.extractUserId(token)).thenReturn(UserConstants.FIRST_USER_ID);
        when(jwtUtils.extractSecurityVersion(token)).thenReturn(1L);
        when(userRepository.findById(UserConstants.FIRST_USER_ID)).thenReturn(Optional.of(user));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtils, never()).extractAuthorities(token);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("When current account is banned should not authenticate an otherwise valid token")
    void whenUserIsBannedShouldNotAuthenticate() throws ServletException, IOException {
        User user = UserTestBuilder.firstUser().banned(true).build();
        when(request.getHeader(ApiConstants.AUTHORIZATION_HEADER)).thenReturn(authorizationHeader);
        when(jwtUtils.isTokenValid(token)).thenReturn(true);
        when(jwtUtils.extractUsername(token)).thenReturn(UserConstants.FIRST_USER_EMAIL);
        when(jwtUtils.extractUserId(token)).thenReturn(UserConstants.FIRST_USER_ID);
        when(userRepository.findById(UserConstants.FIRST_USER_ID)).thenReturn(Optional.of(user));

        jwtRequestFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

  }
