package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryService;
import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;

import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Profile({"local", "test"})
@RequiredArgsConstructor
public class EmailServiceTestImpl implements EmailService {

    private final UserRepository userRepository;
    private final AuthEmailDeliveryService authEmailDeliveryService;
    private final ConcurrentMap<String, UUID> lastActivationTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> lastPasswordResetTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> lastEmailChangeTokens = new ConcurrentHashMap<>();

    @Override
    public void sendActivationEmail(String userEmail, UUID tokenID) {
        lastActivationTokens.put(userEmail.toLowerCase(), tokenID);
        enqueue(userEmail, tokenID, AuthEmailType.ACCOUNT_ACTIVATION);
    }

    @Override
    public void sendPasswordResetEmail(String userEmail, UUID tokenID) {
        lastPasswordResetTokens.put(userEmail.toLowerCase(), tokenID);
        enqueue(userEmail, tokenID, AuthEmailType.PASSWORD_RESET);
    }

    @Override
    public void sendEmailChangeConfirmationEmail(UUID userId, String pendingEmail, UUID tokenID) {
        lastEmailChangeTokens.put(pendingEmail.toLowerCase(), tokenID);
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

    public UUID lastActivationToken(String email) {
        return lastActivationTokens.get(email.toLowerCase());
    }

    public UUID lastPasswordResetToken(String email) {
        return lastPasswordResetTokens.get(email.toLowerCase());
    }

    public UUID lastEmailChangeToken(String email) {
        return lastEmailChangeTokens.get(email.toLowerCase());
    }


}
