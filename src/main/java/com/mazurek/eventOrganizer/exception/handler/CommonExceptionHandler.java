package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class CommonExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(InvalidPageNumberException.class)
    public ResponseEntity<ErrorMessageDto> handleInvalidPageNumberException(InvalidPageNumberException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }
}
