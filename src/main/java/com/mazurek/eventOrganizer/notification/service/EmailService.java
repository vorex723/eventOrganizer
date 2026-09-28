package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.email.AuthEmailType;

import java.util.UUID;

public interface EmailService {
    void sendActivationEmail(String userEmail, UUID tokenID);

    void sendPasswordResetEmail(String userEmail, UUID tokenID);

    boolean wasRecentlyRequested(UUID userId, AuthEmailType type);

    void cancelPendingEmails(UUID userId, AuthEmailType type);
}
