package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterNotificationDeviceDto(
        @NotNull(message = "Device platform must be provided.")
        DevicePlatform platform,

        @NotBlank(message = "Firebase installation id must be provided.")
        @Size(max = 255, message = "Firebase installation id must not exceed 255 characters.")
        @Pattern(
                regexp = "\\S(?:.*\\S)?",
                message = "Firebase installation id must not contain surrounding whitespace."
        )
        String firebaseInstallationId
) {
}
