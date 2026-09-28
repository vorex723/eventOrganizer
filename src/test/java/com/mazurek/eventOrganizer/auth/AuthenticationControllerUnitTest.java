package com.mazurek.eventOrganizer.auth;

import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import com.mazurek.eventOrganizer.utils.DeviceTypeResolver;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticationControllerUnitTest {

    @Test
    void emailChangeConfirmationRedirectsWithTheOutcomeStatus() {
        AuthenticationService authenticationService = mock(AuthenticationService.class);
        UUID token = UUID.randomUUID();
        when(authenticationService.confirmEmailChange(token)).thenReturn(EmailChangeResult.EMAIL_UNAVAILABLE);
        AuthProperties properties = new AuthProperties();
        properties.setEmailChangeResultBaseUrl("https://frontend.example/email-change-result");
        AuthenticationController controller = new AuthenticationController(
                authenticationService,
                mock(DeviceTypeResolver.class),
                properties
        );

        var response = controller.confirmEmailChange(token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SEE_OTHER);
        assertThat(response.getHeaders().getLocation())
                .hasToString("https://frontend.example/email-change-result?status=email_unavailable");
    }
}
