package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.notification.InvalidNotificationPreferencesException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationExceptionHandler unit tests:")
class NotificationExceptionHandlerUnitTest {

    private final NotificationExceptionHandler notificationExceptionHandler =
            new NotificationExceptionHandler();

    @Test
    @DisplayName("When notification preference matrix is invalid should return HTTP 400")
    void whenNotificationPreferenceMatrixIsInvalidShouldReturnHttp400() {
        InvalidNotificationPreferencesException exception =
                new InvalidNotificationPreferencesException();

        ResponseEntity<ErrorMessageDto> response = notificationExceptionHandler
                .handleInvalidNotificationPreferencesException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().getMessage()).isEqualTo(exception.getMessage());
    }
}
