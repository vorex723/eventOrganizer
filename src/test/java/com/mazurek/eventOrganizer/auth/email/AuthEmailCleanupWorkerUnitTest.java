package com.mazurek.eventOrganizer.auth.email;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthEmailCleanupWorkerUnitTest {
    private final AuthEmailMaintenanceService maintenance = mock(AuthEmailMaintenanceService.class);
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(AuthEmailMaintenanceService.class, () -> maintenance)
            .withUserConfiguration(AuthEmailCleanupWorker.class);

    @Test
    void cleanupIsEnabledByDefaultAndDelegatesToTransactionalService() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AuthEmailCleanupWorker.class);
            context.getBean(AuthEmailCleanupWorker.class).cleanup();
            verify(maintenance).cleanup();
        });
    }

    @Test
    void disablingSchedulerKeepsMaintenanceAvailableForExplicitInvocation() {
        runner.withPropertyValues("app.auth.email.cleanup-enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(AuthEmailCleanupWorker.class);
            assertThat(context).hasSingleBean(AuthEmailMaintenanceService.class);
        });
    }
}
