package com.mazurek.eventOrganizer.config.seed;

import com.mazurek.eventOrganizer.config.DataInitializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Local seed profile integration tests:")
class LocalSeedProfileIntegrationTest {
    @ParameterizedTest(name = "[{index}] profile={0}")
    @ValueSource(strings = {"test", "production"})
    void whenNonLocalProfileActiveShouldExcludeSeedingComponents(String profile) {
        new ApplicationContextRunner().withUserConfiguration(DataInitializer.class, LocalDemoSeeder.class, LocalSeedReportPrinter.class)
                .withInitializer(context -> context.getEnvironment().setActiveProfiles(profile))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(DataInitializer.class).doesNotHaveBean(LocalDemoSeeder.class)
                            .doesNotHaveBean(LocalSeedReportPrinter.class);
                });
    }
}
