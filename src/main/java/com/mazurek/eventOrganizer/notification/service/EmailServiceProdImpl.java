package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryService;
import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Profile("production")
public class EmailServiceProdImpl implements EmailService {

    private final UserRepository userRepository;
    private final AuthEmailDeliveryService authEmailDeliveryService;

    @Override
    public void sendActivationEmail(String userEmail, UUID tokenID) {
        enqueue(userEmail, tokenID, AuthEmailType.ACCOUNT_ACTIVATION);
    }

    @Override
    public void sendPasswordResetEmail(String userEmail, UUID tokenID) {
        enqueue(userEmail, tokenID, AuthEmailType.PASSWORD_RESET);
    }

    @Override
    public void sendEmailChangeConfirmationEmail(UUID userId, String pendingEmail, UUID tokenID) {
        authEmailDeliveryService.enqueue(userId, pendingEmail, AuthEmailType.EMAIL_CHANGE_CONFIRMATION, tokenID);
    }

    @Override
    public boolean wasRecentlyRequested(UUID userId, AuthEmailType type) {
        return authEmailDeliveryService.wasRecentlyRequested(userId, type);
    }

    @Override
    public void cancelPendingEmails(UUID userId, AuthEmailType type) {
        authEmailDeliveryService.cancelPending(userId, type);
    }

    private void enqueue(String userEmail, UUID token, AuthEmailType type) {
        userRepository.findByIgnoreCaseEmail(userEmail).ifPresent(user ->
                authEmailDeliveryService.enqueue(user.getId(), user.getEmail(), type, token)
        );
    }
}
