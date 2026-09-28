package com.mazurek.eventOrganizer.notification.delivery;

import java.util.UUID;

public record NotificationDeliveryClaim(
        UUID deliveryId,
        UUID claimToken
) {
}
