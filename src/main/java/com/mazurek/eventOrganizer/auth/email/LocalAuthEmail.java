package com.mazurek.eventOrganizer.auth.email;

import java.time.Instant;

public record LocalAuthEmail(
        AuthEmailType type,
        String recipientEmail,
        String link,
        Instant sentAt
) {
}
