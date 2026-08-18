package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.NotificationTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.InvalidInputConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationTemplateConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationTemplateService integration tests:")
class NotificationTemplateServiceImplIntegrationTest {

    @Autowired
    private NotificationTemplateService notificationTemplateService;

    @Test
    @DisplayName("Should wire notification template service implementation")
    void shouldWireNotificationTemplateServiceImplementation() {
        assertThat(notificationTemplateService).isInstanceOf(NotificationTemplateServiceImpl.class);
    }

    @Test
    @DisplayName("When building private message template should return expected template")
    void whenBuildingPrivateMessageTemplateShouldReturnExpectedTemplate() {
        NotificationTemplate result = notificationTemplateService
                .buildPrivateMessage(UserConstants.FIRST_USER_FULL_NAME);

        assertThat(result).isEqualTo(new NotificationTemplate(
                NotificationTemplateConstants.PRIVATE_MESSAGE_TITLE,
                NotificationTemplateConstants.PRIVATE_MESSAGE_BODY
        ));
    }

    @Test
    @DisplayName("When building new event thread template should return expected template")
    void whenBuildingNewEventThreadTemplateShouldReturnExpectedTemplate() {
        NotificationTemplate result = notificationTemplateService
                .buildNewEventThread(UserConstants.FIRST_USER_FULL_NAME);

        assertThat(result).isEqualTo(new NotificationTemplate(
                NotificationTemplateConstants.NEW_EVENT_THREAD_TITLE,
                NotificationTemplateConstants.NEW_EVENT_THREAD_BODY
        ));
    }

    @Test
    @DisplayName("When building thread reply template should return expected template")
    void whenBuildingThreadReplyTemplateShouldReturnExpectedTemplate() {
        NotificationTemplate result = notificationTemplateService
                .buildThreadReply(UserConstants.FIRST_USER_FULL_NAME);

        assertThat(result).isEqualTo(new NotificationTemplate(
                NotificationTemplateConstants.THREAD_REPLY_TITLE,
                NotificationTemplateConstants.THREAD_REPLY_BODY
        ));
    }

    @Test
    @DisplayName("When building event update template should return expected template")
    void whenBuildingEventUpdateTemplateShouldReturnExpectedTemplate() {
        NotificationTemplate result = notificationTemplateService
                .buildEventUpdate(EventConstants.FIRST_EVENT_NAME);

        assertThat(result).isEqualTo(new NotificationTemplate(
                NotificationTemplateConstants.EVENT_UPDATE_TITLE,
                NotificationTemplateConstants.EVENT_UPDATE_BODY
        ));
    }

    @Test
    @DisplayName("When building new event file template should return expected template")
    void whenBuildingNewEventFileTemplateShouldReturnExpectedTemplate() {
        NotificationTemplate result = notificationTemplateService
                .buildNewEventFile(UserConstants.FIRST_USER_FULL_NAME);

        assertThat(result).isEqualTo(new NotificationTemplate(
                NotificationTemplateConstants.NEW_EVENT_FILE_TITLE,
                NotificationTemplateConstants.NEW_EVENT_FILE_BODY
        ));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
            InvalidInputConstants.EMPTY_VALUE,
            InvalidInputConstants.BLANK_VALUE,
            InvalidInputConstants.WHITESPACE_VALUE
    })
    @DisplayName("When template value is null or blank should reject it through wired service")
    void whenTemplateValueIsNullOrBlankShouldRejectItThroughWiredService(String templateValue) {
        assertThatThrownBy(() -> notificationTemplateService.buildPrivateMessage(templateValue))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
