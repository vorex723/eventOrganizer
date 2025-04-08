package com.mazurek.eventOrganizer.event.dto;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValidationErrorsDto {
    private int status;
    private Map<String, String> errors;

}
