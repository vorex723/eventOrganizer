package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.tag.TagNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class TagExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleTagNotFoundException(TagNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }
}
