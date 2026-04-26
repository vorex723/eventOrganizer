package com.mazurek.eventOrganizer.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token must be provided.")
        String refreshToken) {
}
