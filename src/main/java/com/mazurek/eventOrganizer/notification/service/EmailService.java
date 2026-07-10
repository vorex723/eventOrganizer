package com.mazurek.eventOrganizer.notification.service;

import java.util.UUID;

public interface EmailService {
    void sendActivationEmail(String userEmail, UUID tokenID);
}
