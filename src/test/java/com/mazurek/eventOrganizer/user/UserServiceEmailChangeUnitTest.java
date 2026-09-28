package com.mazurek.eventOrganizer.user;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.auth.EmailChangeService;
import com.mazurek.eventOrganizer.city.CityService;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.user.InvalidPasswordException;
import com.mazurek.eventOrganizer.exception.user.NotMatchingEmailsException;
import com.mazurek.eventOrganizer.exception.user.SameEmailException;
import com.mazurek.eventOrganizer.exception.user.UserAlreadyExistException;
import com.mazurek.eventOrganizer.jwt.JwtUtils;
import com.mazurek.eventOrganizer.jwt.RefreshTokenService;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.dto.ChangeUserEmailDtoTestBuilder;
import com.mazurek.eventOrganizer.user.dto.ChangeUserEmailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.util.Optional;

import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceEmailChangeUnitTest {
    @Mock private UserRepository userRepository;
    @Mock private AuthenticationService authenticationService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private AccountSessionInvalidationService accountSessionInvalidationService;
    @Mock private JwtUtils jwtUtils;
    @Mock private CityService cityService;
    @Mock private Clock clock;
    @Mock private EmailChangeService emailChangeService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private UserService userService;
    private User user;
    private ChangeUserEmailDto request;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                authenticationService,
                refreshTokenService,
                accountSessionInvalidationService,
                jwtUtils,
                cityService,
                passwordEncoder,
                clock,
                emailChangeService
        );
        user = UserTestBuilder.firstUser()
                .password(passwordEncoder.encode(UserConstants.USER_PASSWORD))
                .build();
        request = ChangeUserEmailDtoTestBuilder.validChange()
                .password(UserConstants.USER_PASSWORD)
                .newEmail(UserConstants.FIRST_USER_NEW_EMAIL)
                .newEmailConfirmation(UserConstants.FIRST_USER_NEW_EMAIL)
                .build();
    }

    @Test
    void requestsConfirmationWithoutChangingCredentialsOrSessions() {
        when(authenticationService.getCurrentUser()).thenReturn(user);
        when(userRepository.findByIgnoreCaseEmail(request.getNewEmail())).thenReturn(Optional.empty());

        userService.changeEmail(request);

        verify(emailChangeService).requestChange(user, UserConstants.FIRST_USER_NEW_EMAIL);
        verifyNoInteractions(accountSessionInvalidationService, refreshTokenService, jwtUtils);
        assertThat(user.getEmail()).isEqualTo(UserConstants.FIRST_USER_EMAIL);
    }

    @Test
    void rejectsAnUnauthenticatedRequest() {
        when(authenticationService.getCurrentUser()).thenThrow(new UserNotAuthenticatedException());

        assertThatThrownBy(() -> userService.changeEmail(request))
                .isInstanceOf(UserNotAuthenticatedException.class);
        verifyNoInteractions(emailChangeService);
    }

    @Test
    void rejectsAnIncorrectCurrentPassword() {
        when(authenticationService.getCurrentUser()).thenReturn(user);
        request.setPassword(UserConstants.WRONG_USER_PASSWORD);

        assertThatThrownBy(() -> userService.changeEmail(request))
                .isInstanceOf(InvalidPasswordException.class);
        verifyNoInteractions(emailChangeService);
    }

    @Test
    void rejectsTheCurrentEmailAndMismatchedConfirmation() {
        when(authenticationService.getCurrentUser()).thenReturn(user);
        request.setNewEmail(UserConstants.FIRST_USER_EMAIL.toUpperCase());
        request.setNewEmailConfirmation(UserConstants.FIRST_USER_EMAIL.toUpperCase());

        assertThatThrownBy(() -> userService.changeEmail(request)).isInstanceOf(SameEmailException.class);

        request.setNewEmail(UserConstants.FIRST_USER_NEW_EMAIL);
        request.setNewEmailConfirmation("different@example.com");
        assertThatThrownBy(() -> userService.changeEmail(request)).isInstanceOf(NotMatchingEmailsException.class);
        verifyNoInteractions(emailChangeService);
    }

    @Test
    void rejectsAnAddressAlreadyUsedByAnotherAccount() {
        when(authenticationService.getCurrentUser()).thenReturn(user);
        when(userRepository.findByIgnoreCaseEmail(request.getNewEmail())).thenReturn(Optional.of(UserTestBuilder.secondUser().build()));

        assertThatThrownBy(() -> userService.changeEmail(request))
                .isInstanceOf(UserAlreadyExistException.class);
        verifyNoInteractions(emailChangeService);
    }
}
