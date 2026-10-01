package com.mazurek.eventOrganizer.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ActivationResponse(
        @Schema(allowableValues = {"activated", "expired_resent"}) String status
) {
    public static ActivationResponse activated() {
        return new ActivationResponse("activated");
    }

    public static ActivationResponse expiredResent() {
        return new ActivationResponse("expired_resent");
    }
}
