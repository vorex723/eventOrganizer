package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.auth.AccountAlreadyActivatedException;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenExpiredException;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class AuthExceptionHandler extends BaseDomainExceptionHandler {
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorMessageDto> handleBadCredentialsException(BadCredentialsException exception) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(UserNotAuthenticatedException.class)
    public ResponseEntity<ErrorMessageDto> handleUserNotAuthenticatedException(UserNotAuthenticatedException exception) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(ActivationTokenNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleActivationTokenNotFoundException(ActivationTokenNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(ActivationTokenExpiredException.class)
    public ResponseEntity<ErrorMessageDto> handleActivationTokenExpiredException(ActivationTokenExpiredException exception) {
        return buildErrorResponse(HttpStatus.GONE, exception);
    }

    @ExceptionHandler(AccountAlreadyActivatedException.class)
    public ResponseEntity<ErrorMessageDto> handleAccountAlreadyActivatedException(AccountAlreadyActivatedException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorMessageDto> handleDisabledException(DisabledException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorMessageDto> handleLockedException(LockedException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, exception);
    }
}
