package com.mazurek.eventOrganizer.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

@SpringJUnitConfig(NotificationTemplateServiceImpl.class)
@ActiveProfiles("test")
@DisplayName("Notification template service integration tests:")
class NotificationTemplateServiceImplIntegrationTest {

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Autowired
    private ApplicationContext context;

    @Test
    @DisplayName("When context starts should wire notification template service implementation")
    void whenContextStartsShouldWireNotificationTemplateServiceImplementation() {
        assertThat(context.getBeansOfType(NotificationTemplateService.class))
                .hasSize(1)
                .containsValue(notificationTemplateService);
        assertThat(notificationTemplateService).isInstanceOf(NotificationTemplateServiceImpl.class);
    }
}
