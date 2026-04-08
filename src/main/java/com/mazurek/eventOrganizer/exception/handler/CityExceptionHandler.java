package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.city.CityNotFoundException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class CityExceptionHandler extends BaseDomainExceptionHandler {

    @ExceptionHandler(CityNotFoundException.class)
    public ResponseEntity<ErrorMessageDto> handleCityNotFoundException(CityNotFoundException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }
}
