package com.mazurek.eventOrganizer.notification.dto;

import com.mazurek.eventOrganizer.notification.domain.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterNotificationDeviceDto(
        @NotNull(message = "Device platform must be provided.")
        DevicePlatform platform,

        @NotBlank(message = "Push token must be provided.")
        @Size(max = 1000, message = "Push token must not exceed 1000 characters.")
        String pushToken
) {
}
