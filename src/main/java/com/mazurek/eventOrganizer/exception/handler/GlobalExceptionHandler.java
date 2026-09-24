package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@Order(Ordered.LOWEST_PRECEDENCE)
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorMessageDto> handleOptimisticLockingFailureException(
            OptimisticLockingFailureException exception
    ) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.CONCURRENT_MODIFICATION,
                "The resource was changed by another request. Please retry."
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorMessageDto> handleRuntimeException(RuntimeException exception) {
        log.error("Unhandled runtime exception occurred", exception);
        return buildGenericInternalErrorResponse();
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorMessageDto> handleIOException(IOException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.REQUEST_IO_ERROR, exception);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorMessageDto> handleIllegalArgumentException(IllegalArgumentException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ApiErrorCode.INVALID_ARGUMENT, exception);
    }
}
