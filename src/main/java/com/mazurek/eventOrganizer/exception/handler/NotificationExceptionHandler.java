package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.notification.InvalidNotificationPreferencesException;
import com.mazurek.eventOrganizer.exception.notification.NotificationNotFoundException;
import com.mazurek.eventOrganizer.exception.notification.StaleNotificationPreferencesException;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class NotificationExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(NotificationNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleNotificationNotFoundException(NotificationNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, ApiErrorCode.NOTIFICATION_NOT_FOUND, exception);
    }

    @ExceptionHandler(InvalidNotificationPreferencesException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidNotificationPreferencesException(
            InvalidNotificationPreferencesException exception
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_NOTIFICATION_PREFERENCES, exception);
    }

    @ExceptionHandler(StaleNotificationPreferencesException.class)
    public ResponseEntity<ErrorMessageDto> handleStaleNotificationPreferencesException(
            StaleNotificationPreferencesException exception
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.STALE_NOTIFICATION_PREFERENCES,
                exception.getMessage()
        );
    }
}
