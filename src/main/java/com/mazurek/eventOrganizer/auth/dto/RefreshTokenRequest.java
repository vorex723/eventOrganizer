package com.mazurek.eventOrganizer.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token must be provided.")
        String refreshToken,

        @Size(max = 255, message = "Firebase installation id must not exceed 255 characters.")
        @Pattern(
                regexp = "^$|\\S(?:.*\\S)?",
                message = "Firebase installation id must not contain surrounding whitespace."
        )
        String firebaseInstallationId) {

    public RefreshTokenRequest(String refreshToken) {
        this(refreshToken, null);
    }
}
