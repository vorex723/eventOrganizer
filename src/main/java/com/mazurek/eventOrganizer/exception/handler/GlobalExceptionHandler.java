package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessageDto> handleRuntimeException(RuntimeException exception) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorMessageDto> handleIOException(IOException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessageDto> handleIllegalArgumentException(IllegalArgumentException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }
}
