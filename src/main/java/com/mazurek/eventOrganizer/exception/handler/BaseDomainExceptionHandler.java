package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public abstract class BaseDomainExceptionHandler {

    protected static final String GENERIC_INTERNAL_ERROR_MESSAGE = "Unexpected server error occurred.";

    protected ResponseEntity<ErrorMessageDto> buildErrorResponse(HttpStatus status, Exception exception) {
        return buildErrorResponse(status, exception.getMessage());
    }

    protected ResponseEntity<ErrorMessageDto> buildErrorResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(status.value(), message));
    }

    protected ResponseEntity<ErrorMessageDto> buildGenericInternalErrorResponse() {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, GENERIC_INTERNAL_ERROR_MESSAGE);
    }
}
