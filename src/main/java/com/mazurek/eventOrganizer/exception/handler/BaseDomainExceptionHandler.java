package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public abstract class BaseDomainExceptionHandler {

    protected ResponseEntity<ErrorMessageDto> buildErrorResponse(HttpStatus status, Exception exception) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(status.value(), exception.getMessage()));
    }
}
