package com.mazurek.eventOrganizer.auth.email;

import com.mazurek.eventOrganizer.config.properties.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.auth.email", name = "worker-enabled", havingValue = "true", matchIfMissing = true)
public class AuthEmailDeliveryWorker {

    private final AuthEmailDeliveryService authEmailDeliveryService;

    @Scheduled(fixedDelayString = "${app.auth.email.poll-delay:5s}")
    public void processPendingDeliveries() {
        authEmailDeliveryService.processPendingDeliveries();
    }
}
