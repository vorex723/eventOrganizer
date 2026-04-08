package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.jwt.InvalidRefreshTokenException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenExpiredException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenNotFoundException;
import com.mazurek.eventOrganizer.exception.jwt.RefreshTokenRevokedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class JwtExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidRefreshTokenException(InvalidRefreshTokenException exception) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(RefreshTokenNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleRefreshTokenNotFoundException(RefreshTokenNotFoundException exception) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(RefreshTokenExpiredException.class)
    public ResponseEntity<ErrorMessageDto> handleRefreshTokenExpiredException(RefreshTokenExpiredException exception) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, exception);
    }

    @ExceptionHandler(RefreshTokenRevokedException.class)
    public ResponseEntity<ErrorMessageDto> handleRefreshTokenRevokedException(RefreshTokenRevokedException exception) {
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, exception);
    }
}
