package com.mazurek.eventOrganizer.auth.email;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@DisplayName("Auth email cleanup worker integration tests:")
class AuthEmailCleanupWorkerIntegrationTest {
    private final AuthEmailMaintenanceService maintenance = mock(AuthEmailMaintenanceService.class);
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(AuthEmailMaintenanceService.class, () -> maintenance)
            .withUserConfiguration(AuthEmailCleanupWorker.class);

    @Test
    void whenCleanupIsEnabledByDefaultShouldDelegateToMaintenanceService() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AuthEmailCleanupWorker.class);
            context.getBean(AuthEmailCleanupWorker.class).cleanup();
            verify(maintenance).cleanup();
        });
    }

    @Test
    void whenCleanupSchedulerIsDisabledShouldKeepMaintenanceAvailable() {
        runner.withPropertyValues("app.auth.email.cleanup-enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(AuthEmailCleanupWorker.class);
            assertThat(context).hasSingleBean(AuthEmailMaintenanceService.class);
        });
    }
}
