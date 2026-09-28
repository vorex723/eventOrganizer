package com.mazurek.eventOrganizer.config;

import com.mazurek.eventOrganizer.exception.ApiErrorCode;
import com.mazurek.eventOrganizer.exception.ErrorMessageDto;
import jakarta.validation.ConstraintViolationException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return buildErrorResponse(ApiErrorCode.MALFORMED_REQUEST, "Request body is malformed.");
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return buildErrorResponse(ApiErrorCode.VALIDATION_FAILED, "A required request parameter is missing.");
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        return buildErrorResponse(ApiErrorCode.MALFORMED_REQUEST, "A request parameter has an invalid value.");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(ConstraintViolationException ex) {
        return buildErrorResponse(ApiErrorCode.VALIDATION_FAILED, "Request validation failed");
    }

    private void putValidationError(Map<String, String> errors, ObjectError error) {
        String fieldName = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
        errors.put(fieldName, error.getDefaultMessage());
    }

    private ResponseEntity<Object> buildValidationErrorResponse(Map<String, String> errors) {
        ErrorMessageDto error = error(ApiErrorCode.VALIDATION_FAILED, "Request validation failed");
        error.setErrors(errors);
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<Object> buildErrorResponse(String code, String message) {
        return new ResponseEntity<>(error(code, message), HttpStatus.BAD_REQUEST);
    }

    private ErrorMessageDto error(String code, String message) {
        return new ErrorMessageDto(HttpStatus.BAD_REQUEST.value(), code, message);
    }
}
