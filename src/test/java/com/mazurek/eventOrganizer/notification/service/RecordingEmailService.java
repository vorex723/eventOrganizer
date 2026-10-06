package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.email.AuthEmailDeliveryService;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@Profile("test")
public class RecordingEmailService extends EmailServiceImpl {

    private final ConcurrentMap<String, UUID> lastActivationTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> lastPasswordResetTokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> lastEmailChangeTokens = new ConcurrentHashMap<>();

    public RecordingEmailService(UserRepository userRepository, AuthEmailDeliveryService authEmailDeliveryService) {
        super(userRepository, authEmailDeliveryService);
    }

    @Override
    public void sendActivationEmail(String userEmail, UUID tokenID) {
        lastActivationTokens.put(userEmail.toLowerCase(Locale.ROOT), tokenID);
        super.sendActivationEmail(userEmail, tokenID);
    }

    @Override
    public void sendPasswordResetEmail(String userEmail, UUID tokenID) {
        lastPasswordResetTokens.put(userEmail.toLowerCase(Locale.ROOT), tokenID);
        super.sendPasswordResetEmail(userEmail, tokenID);
    }

    @Override
    public void sendEmailChangeConfirmationEmail(UUID userId, String pendingEmail, UUID tokenID) {
        lastEmailChangeTokens.put(pendingEmail.toLowerCase(Locale.ROOT), tokenID);
        super.sendEmailChangeConfirmationEmail(userId, pendingEmail, tokenID);
    }

    public UUID lastActivationToken(String email) {
        return lastActivationTokens.get(email.toLowerCase(Locale.ROOT));
    }

    public UUID lastPasswordResetToken(String email) {
        return lastPasswordResetTokens.get(email.toLowerCase(Locale.ROOT));
    }

    public UUID lastEmailChangeToken(String email) {
        return lastEmailChangeTokens.get(email.toLowerCase(Locale.ROOT));
    }

    /** Clears recordings between test cases; does not change the database/outbox. */
    public void reset() {
        lastActivationTokens.clear();
        lastPasswordResetTokens.clear();
        lastEmailChangeTokens.clear();
    }
}
