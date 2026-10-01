package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.auth.EmailChangeAddressUnavailableException;
import com.mazurek.eventOrganizer.exception.auth.EmailChangeTokenExpiredException;
import com.mazurek.eventOrganizer.exception.auth.EmailChangeTokenInvalidException;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthenticationControllerUnitTest {

    @ParameterizedTest
    @CsvSource({"ACTIVATED, activated", "TOKEN_EXPIRED_NEW_SENT, expired_resent"})
    void activationReturnsTheStableJsonStatusContract(ActivationResult result, String expectedStatus) {
        AuthenticationService service = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        when(service.activateAccount(token)).thenReturn(result);

        var response = controller(service).activateAccount(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(expectedStatus);
        assertThat(response.getHeaders().getLocation()).isNull();
        verify(service).activateAccount(token);
    }

    @Test
    void emailChangeReturnsJsonWithoutRedirecting() {
        AuthenticationService service = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        when(service.confirmEmailChange(token)).thenReturn(EmailChangeResult.CHANGED);

        var response = controller(service).confirmEmailChange(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("changed");
        assertThat(response.getHeaders().getLocation()).isNull();
        verify(service).confirmEmailChange(token);
    }

    @Test
    void expiredEmailChangeOutcomeIsMappedAfterTheServiceReturns() {
        AuthenticationService service = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        when(service.confirmEmailChange(token)).thenReturn(EmailChangeResult.EXPIRED);

        assertThatThrownBy(() -> controller(service).confirmEmailChange(token))
                .isInstanceOf(EmailChangeTokenExpiredException.class);
        verify(service).confirmEmailChange(token);
    }

    @Test
    void invalidEmailChangeOutcomeUsesTheSharedErrorHandler() {
        AuthenticationService service = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        when(service.confirmEmailChange(token)).thenReturn(EmailChangeResult.INVALID_TOKEN);

        assertThatThrownBy(() -> controller(service).confirmEmailChange(token))
                .isInstanceOf(EmailChangeTokenInvalidException.class);
    }

    @Test
    void unavailableEmailChangeOutcomeUsesTheSharedErrorHandler() {
        AuthenticationService service = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        when(service.confirmEmailChange(token)).thenReturn(EmailChangeResult.EMAIL_UNAVAILABLE);

        assertThatThrownBy(() -> controller(service).confirmEmailChange(token))
                .isInstanceOf(EmailChangeAddressUnavailableException.class);
    }

    @Test
    void racedEmailChangeConflictIsPassedToTheSharedErrorHandler() {
        AuthenticationService service = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        EmailChangeAddressUnavailableException conflict = new EmailChangeAddressUnavailableException();
        when(service.confirmEmailChange(token)).thenThrow(conflict);

        assertThatThrownBy(() -> controller(service).confirmEmailChange(token)).isSameAs(conflict);
    }

    @Test
    void invalidActivationResultUsesTheSharedErrorHandler() {
        AuthenticationService service = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        when(service.activateAccount(token)).thenReturn(ActivationResult.INVALID_TOKEN);

        assertThatThrownBy(() -> controller(service).activateAccount(token))
                .isInstanceOf(ActivationTokenNotFoundException.class);
    }

    @Test
    void missingActivationTokenUsesTheSharedErrorHandler() {
        AuthenticationService service = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        ActivationTokenNotFoundException invalid = new ActivationTokenNotFoundException();
        when(service.activateAccount(token)).thenThrow(invalid);

        assertThatThrownBy(() -> controller(service).activateAccount(token)).isSameAs(invalid);
    }

    private AuthenticationController controller(AuthenticationService service) {
        return new AuthenticationController(service, mock(DeviceTypeResolver.class));
    }
}
