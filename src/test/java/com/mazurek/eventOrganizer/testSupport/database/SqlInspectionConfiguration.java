package com.mazurek.eventOrganizer.testSupport.database;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Explicitly imported by SQL-sensitive integration tests, not global test wiring. */
@TestConfiguration(proxyBeanMethods = false)
public class SqlInspectionConfiguration {
    @Bean
    SqlCapture sqlCapture() {
        return new SqlCapture();
    }

    @Bean
    HibernatePropertiesCustomizer associationSqlInspector(SqlCapture sqlCapture) {
        return properties -> properties.put(AvailableSettings.STATEMENT_INSPECTOR, sqlCapture);
    }
}
