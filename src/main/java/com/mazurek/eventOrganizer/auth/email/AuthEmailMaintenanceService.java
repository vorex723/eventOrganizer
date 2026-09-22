package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.auth.PasswordResetTokenRepository;
import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "app.auth.email", name = "cleanup-enabled", havingValue = "true", matchIfMissing = true)
public class AuthEmailMaintenanceService {

    private final ActivationTokenRepository activationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthEmailDeliveryRepository authEmailDeliveryRepository;
    private final AuthProperties authProperties;
    private final Clock clock;

    @Scheduled(cron = "${app.auth.email.cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void cleanup() {
        Instant now = clock.instant();
        int expiredActivations = activationTokenRepository.deleteExpiredBefore(now);
        int expiredResets = passwordResetTokenRepository.deleteExpiredBefore(now);
        int deliveries = authEmailDeliveryRepository.deleteCompletedBefore(
                now.minus(authProperties.getEmail().getRetention())
        );

        if (expiredActivations + expiredResets + deliveries > 0) {
            log.info(
                    "Deleted {} expired activation tokens, {} expired password reset tokens, and {} completed auth email deliveries.",
                    expiredActivations,
                    expiredResets,
                    deliveries
            );
        }
    }
}
