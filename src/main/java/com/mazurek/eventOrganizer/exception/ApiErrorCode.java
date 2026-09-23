package com.mazurek.eventOrganizer.exception;

import org.springframework.http.HttpStatus;

public final class ApiErrorCode {

    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String AUTHENTICATION_REQUIRED = "AUTHENTICATION_REQUIRED";
    public static final String INVALID_ACCESS_TOKEN = "INVALID_ACCESS_TOKEN";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private ApiErrorCode() {
    }

    public static String forStatus(int status) {
        return switch (status) {
            case 400 -> "BAD_REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT_FOUND";
            case 409 -> "CONFLICT";
            case 410 -> "GONE";
            default -> status >= HttpStatus.INTERNAL_SERVER_ERROR.value() ? INTERNAL_ERROR : "REQUEST_FAILED";
        };
    }
}
