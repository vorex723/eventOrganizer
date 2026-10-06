package com.mazurek.eventOrganizer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.health.DataRedisHealthContributorAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.health.DataRedisReactiveHealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.application.AvailabilityHealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.application.DiskSpaceHealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.application.SslHealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.contributor.HealthContributorAutoConfiguration;
import org.springframework.boot.health.autoconfigure.registry.HealthContributorRegistryAutoConfiguration;
import org.springframework.boot.health.contributor.HealthContributors;
import org.springframework.boot.health.contributor.ReactiveHealthContributors;
import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.health.registry.ReactiveHealthContributorRegistry;
import org.springframework.boot.jdbc.autoconfigure.health.DataSourceHealthContributorAutoConfiguration;
import org.springframework.boot.mail.autoconfigure.MailHealthContributorAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("Health check profile configuration integration tests:")
class HealthCheckProfileConfigurationIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataSourceHealthContributorAutoConfiguration.class,
                    DataRedisHealthContributorAutoConfiguration.class,
                    DataRedisReactiveHealthContributorAutoConfiguration.class,
                    MailHealthContributorAutoConfiguration.class,
                    AvailabilityHealthContributorAutoConfiguration.class,
                    DiskSpaceHealthContributorAutoConfiguration.class,
                    SslHealthContributorAutoConfiguration.class,
                    HealthContributorAutoConfiguration.class,
                    HealthContributorRegistryAutoConfiguration.class
            ))
            .withBean(DataSource.class, () -> mock(DataSource.class))
            .withBean(LettuceConnectionFactory.class, () -> mock(LettuceConnectionFactory.class))
            .withBean(JavaMailSenderImpl.class, () -> mock(JavaMailSenderImpl.class));

    @ParameterizedTest(name = "[{index}] profile={0}")
    @ValueSource(strings = {"local", "test"})
    void whenProfileIsNonProductionShouldCheckOnlyDatabase(String profile) throws IOException {
        profile(profile).run(context -> {
            assertThat(context).hasNotFailed().hasBean("dbHealthContributor").doesNotHaveBean("redisHealthContributor");
            assertThat(contributorNames(context.getBean(HealthContributorRegistry.class),
                    context.getBean(ReactiveHealthContributorRegistry.class))).containsExactly("db");
        });
    }

    @Test
    void whenProductionRateLimiterIsEnabledShouldCheckDatabaseAndRedis() throws IOException {
        profile("production").run(context -> {
            assertThat(context).hasNotFailed().hasBean("dbHealthContributor").hasBean("redisHealthContributor");
            assertThat(contributorNames(context.getBean(HealthContributorRegistry.class),
                    context.getBean(ReactiveHealthContributorRegistry.class))).containsExactlyInAnyOrder("db", "redis");
        });
    }

    @Test
    void whenProductionRateLimiterIsDisabledShouldNotRequireRedis() throws IOException {
        profile("production").withPropertyValues("app.auth.rate-limit.enabled=false").run(context -> {
            assertThat(context).hasNotFailed().hasBean("dbHealthContributor").doesNotHaveBean("redisHealthContributor");
            assertThat(contributorNames(context.getBean(HealthContributorRegistry.class),
                    context.getBean(ReactiveHealthContributorRegistry.class))).containsExactly("db");
        });
    }

    private ApplicationContextRunner profile(String profile) throws IOException {
        String profileFile = profile.equals("test")
                ? "src/test/resources/application-test.properties"
                : "src/main/resources/application-" + profile + ".properties";
        var profileProperties = PropertiesLoaderUtils.loadProperties(new FileSystemResource(profileFile));
        var applicationProperties = PropertiesLoaderUtils.loadProperties(
                new FileSystemResource("src/main/resources/application.properties"));
        return contextRunner.withInitializer(context -> {
            var environment = context.getEnvironment();
            environment.setActiveProfiles(profile);
            environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
            environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
            environment.getPropertySources().addLast(new PropertiesPropertySource("profileConfiguration", profileProperties));
            environment.getPropertySources().addLast(new PropertiesPropertySource("applicationDefaults", applicationProperties));
        });
    }

    private List<String> contributorNames(HealthContributorRegistry blocking, ReactiveHealthContributorRegistry reactive) {
        return Stream.concat(blocking.stream().map(HealthContributors.Entry::name),
                reactive.stream().map(ReactiveHealthContributors.Entry::name)).distinct().toList();
    }
}
