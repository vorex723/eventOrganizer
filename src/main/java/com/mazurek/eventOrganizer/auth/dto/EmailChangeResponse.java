package com.mazurek.eventOrganizer.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailChangeResponse(
        @Schema(allowableValues = "changed") String status
) {
    public static EmailChangeResponse changed() {
        return new EmailChangeResponse("changed");
    }
}
