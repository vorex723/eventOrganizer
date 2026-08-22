package com.mazurek.eventOrganizer.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


import java.util.List;

public record UpdateNotificationPreferencesDto(
        @NotEmpty
        List<@NotNull @Valid UpdateNotificationPreferenceDto> preferences
) {
}

