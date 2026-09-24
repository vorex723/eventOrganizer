package com.mazurek.eventOrganizer.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

public record UpdateNotificationPreferencesDto(
        @NotNull
        @PositiveOrZero
        Long version,
        @NotEmpty
        List<@NotNull @Valid UpdateNotificationPreferenceDto> preferences
) {
}
