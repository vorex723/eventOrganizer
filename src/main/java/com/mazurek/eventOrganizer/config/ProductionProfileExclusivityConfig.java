package com.mazurek.eventOrganizer.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("production & (local | test)")
public class ProductionProfileExclusivityConfig {

    @Bean
    static BeanFactoryPostProcessor rejectProductionWithNonProductionProfiles() {
        return beanFactory -> {
            throw new IllegalStateException(
                    "The production profile cannot be combined with local or test."
            );
        };
    }
}
