package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.exception.auth.EmailChangeAddressUnavailableException;
import com.mazurek.eventOrganizer.exception.auth.ActivationTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.auth.EmailChangeTokenExpiredException;
import com.mazurek.eventOrganizer.exception.auth.EmailChangeTokenInvalidException;
import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.exception.auth.PasswordResetTokenNotFoundException;
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
    @ExceptionHandler(ActivationTokenNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleActivationTokenNotFoundException(ActivationTokenNotFoundException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.ACTIVATION_TOKEN_INVALID, exception);
    }

    @ExceptionHandler(EmailChangeTokenInvalidException.class)
    public ResponseEntity<ErrorMessageDto> handleEmailChangeTokenInvalidException(EmailChangeTokenInvalidException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.EMAIL_CHANGE_TOKEN_INVALID, exception);
    }

    @ExceptionHandler(EmailChangeTokenExpiredException.class)
    public ResponseEntity<ErrorMessageDto> handleEmailChangeTokenExpiredException(EmailChangeTokenExpiredException exception) {
        return buildErrorResponse(HttpStatus.GONE, ApiErrorCode.EMAIL_CHANGE_TOKEN_EXPIRED, exception);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorMessageDto> handleBadCredentialsException(BadCredentialsException exception) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ApiErrorCode.INVALID_CREDENTIALS, exception);
    }

    @ExceptionHandler(UserNotAuthenticatedException.class)
    public ResponseEntity<ErrorMessageDto> handleUserNotAuthenticatedException(UserNotAuthenticatedException exception) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ApiErrorCode.AUTHENTICATION_REQUIRED, exception);
    }

    @ExceptionHandler(PasswordResetTokenNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handlePasswordResetTokenNotFoundException(
            PasswordResetTokenNotFoundException exception
    ) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.PASSWORD_RESET_TOKEN_INVALID, exception);
    }

    @ExceptionHandler(EmailChangeAddressUnavailableException.class)
    public ResponseEntity<ErrorMessageDto> handleEmailChangeAddressUnavailableException(
            EmailChangeAddressUnavailableException exception
    ) {
        return buildErrorResponse(HttpStatus.CONFLICT, ApiErrorCode.EMAIL_CHANGE_ADDRESS_UNAVAILABLE, exception);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorMessageDto> handleDisabledException(DisabledException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ApiErrorCode.ACCOUNT_DISABLED, exception);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorMessageDto> handleLockedException(LockedException exception) {
        return buildErrorResponse(HttpStatus.FORBIDDEN, ApiErrorCode.ACCOUNT_LOCKED, exception);
    }
}
