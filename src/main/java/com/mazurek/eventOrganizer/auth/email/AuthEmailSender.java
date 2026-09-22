package com.mazurek.eventOrganizer.auth.email;

public interface AuthEmailSender {
    AuthEmailSendResult send(AuthEmailType type, String recipientEmail, String rawToken);
}
