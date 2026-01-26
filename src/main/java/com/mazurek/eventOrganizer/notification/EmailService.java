package com.mazurek.eventOrganizer.notification;

import java.util.UUID;

public interface EmailService {
    void sendActivationEmail(String userEmail, UUID tokenID);
}
