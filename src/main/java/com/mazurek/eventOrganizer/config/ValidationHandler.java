package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class ValidationHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> putValidationError(errors, error));

        return buildValidationErrorResponse(errors);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Object> handleBindException(BindException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> putValidationError(errors, error));

        return buildValidationErrorResponse(errors);
    }

    private void putValidationError(Map<String, String> errors, ObjectError error) {
        String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
        errors.put(fieldName, error.getDefaultMessage());
    }

    private ResponseEntity<Object> buildValidationErrorResponse(Map<String, String> errors) {
        ErrorMessageDto error = new ErrorMessageDto(
                HttpStatus.BAD_REQUEST.value(),
                ApiErrorCode.VALIDATION_FAILED,
                "Request validation failed"
        );
        error.setErrors(errors);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
