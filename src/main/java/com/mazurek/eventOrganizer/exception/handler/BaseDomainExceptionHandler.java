package com.mazurek.eventOrganizer.exception.handler;

import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public abstract class BaseDomainExceptionHandler {

    protected static final String GENERIC_INTERNAL_ERROR_MESSAGE = "Unexpected server error occurred.";

    protected ResponseEntity<ErrorMessageDto> buildErrorResponse(HttpStatus status, Exception exception) {
        return buildErrorResponse(status, exception.getMessage());
    }

    protected ResponseEntity<ErrorMessageDto> buildErrorResponse(HttpStatus status, String message) {
        return buildErrorResponse(status, ApiErrorCode.forStatus(status.value()), message);
    }

    protected ResponseEntity<ErrorMessageDto> buildErrorResponse(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(status.value(), code, message));
    }

    protected ResponseEntity<ErrorMessageDto> buildGenericInternalErrorResponse() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ErrorMessageDto(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        ApiErrorCode.INTERNAL_ERROR,
                        GENERIC_INTERNAL_ERROR_MESSAGE
                ));
    }
}
