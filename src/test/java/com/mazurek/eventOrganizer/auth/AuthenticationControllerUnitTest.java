package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationControllerUnitTest {

    @ParameterizedTest
    @CsvSource({
            "ACTIVATED, activated",
            "TOKEN_EXPIRED_NEW_SENT, expired_resent",
            "INVALID_TOKEN, invalid_token"
    })
    void activationRedirectUsesTheStableFrontendStatusContract(
            ActivationResult result,
            String expectedStatus
    ) {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        when(authenticationService.activateAccount(token)).thenReturn(result);
        AuthenticationController controller = controller(authenticationService);

        var response = controller.activateAccount(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(response.getHeaders().getLocation())
                .hasToString("https://frontend.example/activation-result?status=" + expectedStatus);
    }

    @ParameterizedTest
    @CsvSource({
            "CHANGED, changed",
            "EXPIRED, expired",
            "INVALID_TOKEN, invalid_token",
            "EMAIL_UNAVAILABLE, email_unavailable"
    })
    void emailChangeRedirectUsesTheStableFrontendStatusContract(
            EmailChangeResult result,
            String expectedStatus
    ) {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        when(authenticationService.confirmEmailChange(token)).thenReturn(result);
        AuthenticationController controller = controller(authenticationService);

        var response = controller.confirmEmailChange(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(response.getHeaders().getLocation())
                .hasToString("https://frontend.example/email-change-result?status=" + expectedStatus);
    }

    @Test
    void missingActivationTokenUsesTheInvalidTokenRedirectContract() {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        when(authenticationService.activateAccount(token)).thenThrow(new ActivationTokenNotFoundException());

        var response = controller(authenticationService).activateAccount(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(response.getHeaders().getLocation())
                .hasToString("https://frontend.example/activation-result?status=invalid_token");
    }

    private AuthenticationController controller(AuthenticationService authenticationService) {
        AuthProperties properties = new AuthProperties();
        properties.setActivationResultBaseUrl("https://frontend.example/activation-result");
        properties.setEmailChangeResultBaseUrl("https://frontend.example/email-change-result");
        return new AuthenticationController(
                authenticationService,
                mock(DeviceTypeResolver.class),
                properties
        );
    }
}
