package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.config.properties.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;

@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalAuthEmailSink {

    private static final int MAX_DELIVERIES = 100;

    private final MailProperties mailProperties;
    private final Clock clock;
    private final ConcurrentLinkedDeque<LocalAuthEmail> deliveries = new ConcurrentLinkedDeque<>();

    public LocalAuthEmail record(AuthEmailType type, String recipientEmail, String rawToken) {
        LocalAuthEmail delivery = new LocalAuthEmail(
                type,
                recipientEmail,
                linkFor(type, rawToken),
                clock.instant()
        );
        deliveries.addFirst(delivery);
        while (deliveries.size() > MAX_DELIVERIES) {
            deliveries.pollLast();
        }
        return delivery;
    }

    public List<LocalAuthEmail> recent() {
        return new ArrayList<>(deliveries);
    }

    private String linkFor(AuthEmailType type, String rawToken) {
        return switch (type) {
            case ACCOUNT_ACTIVATION -> mailProperties.getActivationBaseUrl() + rawToken;
            case PASSWORD_RESET -> mailProperties.getPasswordResetBaseUrl() + rawToken;
            case EMAIL_CHANGE_CONFIRMATION -> mailProperties.getEmailChangeBaseUrl() + rawToken;
        };
    }
}
