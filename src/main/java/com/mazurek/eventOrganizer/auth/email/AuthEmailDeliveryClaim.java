package com.mazurek.eventOrganizer.auth.email;

import java.util.UUID;

public record AuthEmailDeliveryClaim(UUID deliveryId, UUID claimToken) {
}
