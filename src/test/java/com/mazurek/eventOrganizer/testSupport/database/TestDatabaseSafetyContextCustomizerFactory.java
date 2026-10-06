package com.mazurek.eventOrganizer.testSupport.database;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;

import javax.sql.DataSource;
import java.util.List;

/** Registered for Spring TestContext only, never packaged with the application. */
public final class TestDatabaseSafetyContextCustomizerFactory implements ContextCustomizerFactory {
    @Override
    public ContextCustomizer createContextCustomizer(Class<?> testClass,
                                                     List<ContextConfigurationAttributes> configAttributes) {
        return SafetyCustomizer.INSTANCE;
    }

    // Stable equality preserves Spring's cache reuse across compatible test classes.
    private enum SafetyCustomizer implements ContextCustomizer {
        INSTANCE;

        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                                     MergedContextConfiguration mergedConfig) {
            context.addBeanFactoryPostProcessor(beanFactory -> {
                if (beanFactory.getBeanNamesForType(DataSource.class, true, false).length > 0
                        || beanFactory.getBeanNamesForType(Flyway.class, true, false).length > 0) {
                    requireSafeConfiguration(context.getEnvironment());
                }
                beanFactory.addBeanPostProcessor(new BeanPostProcessor() {
                    @Override
                    public Object postProcessAfterInitialization(Object bean, String beanName) {
                        if (bean instanceof DataSource dataSource) {
                            TestDatabaseSafety.requireSafeDataSource(dataSource);
                        }
                        if (bean instanceof Flyway flyway) {
                            TestDatabaseSafety.requireSafeDataSource(flyway.getConfiguration().getDataSource());
                            for (String schema : flyway.getConfiguration().getSchemas()) {
                                TestDatabaseSafety.requireSafeSchema(schema);
                            }
                            if (flyway.getConfiguration().getDefaultSchema() != null) {
                                TestDatabaseSafety.requireSafeSchema(flyway.getConfiguration().getDefaultSchema());
                            }
                        }
                        return bean;
                    }
                });
            });
        }
    }

    private static void requireSafeConfiguration(Environment environment) {
        String url = environment.getProperty("spring.datasource.url");
        String username = environment.getProperty("spring.datasource.username");
        TestDatabaseSafety.requireSafeTarget(url, username);
        if (environment.containsProperty("spring.datasource.hikari.jdbc-url")
                || environment.containsProperty("spring.datasource.hikari.username")) {
            TestDatabaseSafety.requireSafeTarget(environment.getProperty("spring.datasource.hikari.jdbc-url", url),
                    environment.getProperty("spring.datasource.hikari.username", username));
        }
        if (environment.containsProperty("spring.flyway.url") || environment.containsProperty("spring.flyway.user")) {
            TestDatabaseSafety.requireSafeTarget(environment.getProperty("spring.flyway.url", url),
                    environment.getProperty("spring.flyway.user", username));
        }
        for (String key : List.of("spring.flyway.schemas", "spring.flyway.default-schema",
                "spring.datasource.hikari.schema")) {
            String schemas = environment.getProperty(key);
            if (schemas != null) {
                if (key.equals("spring.flyway.schemas")) {
                    for (String schema : schemas.split(",", -1)) TestDatabaseSafety.requireSafeSchema(schema);
                } else {
                    TestDatabaseSafety.requireSafeSchema(schemas);
                }
            }
        }
        for (String key : List.of("spring.datasource.jndi-name", "spring.datasource.hikari.data-source-class-name",
                "spring.datasource.hikari.data-source-jndi", "spring.datasource.hikari.data-source-j-n-d-i",
                "spring.datasource.hikari.connection-init-sql", "spring.datasource.hikari.connection-test-query")) {
            if (environment.containsProperty(key)) {
                throw new IllegalStateException("Unsafe test database: indirect connection configuration is forbidden");
            }
        }
    }
}
