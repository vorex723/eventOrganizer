package com.mazurek.eventOrganizer.auth.email;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.auth.email", name = "cleanup-enabled", havingValue = "true", matchIfMissing = true)
public class AuthEmailCleanupWorker {
    private final AuthEmailMaintenanceService maintenanceService;

    @Scheduled(cron = "${app.auth.email.cleanup-cron:0 30 3 * * *}")
    public void cleanup() {
        maintenanceService.cleanup();
    }
}
