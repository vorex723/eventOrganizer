package com.mazurek.eventOrganizer.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ErrorMessageDto {
    private int status;
    private String code;
    private String message;
    private Map<String, String> errors;

    public ErrorMessageDto(int status, String code, String message) {
        this(status, code, message, null);
    }

    public ErrorMessageDto(int status, String message) {
        this(status, ApiErrorCode.forStatus(status), message, null);
    }
}
