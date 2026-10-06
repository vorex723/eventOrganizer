package com.mazurek.eventOrganizer.testData;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtUserDetails;
import com.mazurek.eventOrganizer.notification.service.RecordingEmailService;
import com.mazurek.eventOrganizer.testData.builders.RoleTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.Set;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthHelper unit tests:")
class AuthHelperUnitTest {
    @InjectMocks private AuthHelper authHelper;
    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AuthenticationService authenticationService;
    @Mock private ActivationTokenRepository activationTokenRepository;
    @Mock private RecordingEmailService emailService;

    @BeforeEach
    void seedPreviousActor() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(UserConstants.THIRD_USER_EMAIL, null, RoleConstants.ROLE_USER_NAME));
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "Second user: {0}")
    @ValueSource(booleans = {false, true})
    void whenActorExistsShouldReplacePreviousAuthenticationAndKeepRequestedPrincipal(boolean secondUser) {
        User user = (secondUser ? UserTestBuilder.secondUser() : UserTestBuilder.firstUser())
                .roles(Set.of(RoleTestBuilder.userRole().build(), RoleTestBuilder.adminRole().build()))
                .build();
        when(userRepository.findByIgnoreCaseEmail(email(secondUser))).thenReturn(Optional.of(user));

        setupActor(secondUser);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal()).isInstanceOfSatisfying(JwtUserDetails.class, principal -> {
            assertThat(principal.getId()).isEqualTo(user.getId());
            assertThat(principal.getUsername()).isEqualTo(email(secondUser));
            assertThat(principal.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                    .containsExactlyInAnyOrder(RoleConstants.ROLE_USER_NAME, RoleConstants.ROLE_ADMIN_NAME);
        });
    }

    @ParameterizedTest(name = "Second user: {0}")
    @ValueSource(booleans = {false, true})
    void whenActorIsMissingShouldLeaveNoPreviousAuthentication(boolean secondUser) {
        when(userRepository.findByIgnoreCaseEmail(email(secondUser))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> setupActor(secondUser)).isInstanceOf(UserNotFoundException.class);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @ParameterizedTest(name = "Second user: {0}")
    @ValueSource(booleans = {false, true})
    void whenLookupFailsShouldPropagateFailureAndLeaveNoPreviousAuthentication(boolean secondUser) {
        IllegalStateException failure = new IllegalStateException("Fixture user lookup failure");
        when(userRepository.findByIgnoreCaseEmail(email(secondUser))).thenThrow(failure);

        assertThatThrownBy(() -> setupActor(secondUser)).isSameAs(failure);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void setupActor(boolean secondUser) {
        if (secondUser) authHelper.setupSecurityContextForSecondUser();
        else authHelper.setupSecurityContextForFirstUser();
    }

    private String email(boolean secondUser) {
        return secondUser ? UserConstants.SECOND_USER_EMAIL : UserConstants.FIRST_USER_EMAIL;
    }
}
