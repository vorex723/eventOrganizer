package com.mazurek.eventOrganizer.user.dto;

import jakarta.validation.constraints.NotBlank;

public record DeleteCurrentUserDto(
        @NotBlank(message = "Password is required.")
        String password
) {
}
