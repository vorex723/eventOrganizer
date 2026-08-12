package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.NotificationTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.mazurek.eventOrganizer.testData.TestConstants.EventConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.InvalidInputConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationTemplateConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NotificationTemplateServiceImpl unit tests:")
class NotificationTemplateServiceImplUnitTest {

    private NotificationTemplateService notificationTemplateService;

    @BeforeEach
    void setUp() {
        notificationTemplateService = new NotificationTemplateServiceImpl();
    }

    @Nested
    @DisplayName("Build private message template tests:")
    class BuildPrivateMessageTests {

        @Test
        @DisplayName("When building private message template should return expected title and body")
        void whenBuildingPrivateMessageTemplateShouldReturnExpectedTitleAndBody() {
            NotificationTemplate result = notificationTemplateService
                    .buildPrivateMessage(UserConstants.FIRST_USER_FULL_NAME);

            assertThat(result).isEqualTo(new NotificationTemplate(
                    NotificationTemplateConstants.PRIVATE_MESSAGE_TITLE,
                    NotificationTemplateConstants.PRIVATE_MESSAGE_BODY
            ));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {
                InvalidInputConstants.EMPTY_VALUE,
                InvalidInputConstants.BLANK_VALUE,
                InvalidInputConstants.WHITESPACE_VALUE
        })
        @DisplayName("When sender full name is null or blank should throw IllegalArgumentException")
        void whenSenderFullNameIsNullOrBlankShouldThrowIllegalArgumentException(String senderFullName) {
            assertThatThrownBy(() -> notificationTemplateService.buildPrivateMessage(senderFullName))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Build new event thread template tests:")
    class BuildNewEventThreadTests {

        @Test
        @DisplayName("When building new event thread template should return expected title and body")
        void whenBuildingNewEventThreadTemplateShouldReturnExpectedTitleAndBody() {
            NotificationTemplate result = notificationTemplateService
                    .buildNewEventThread(UserConstants.FIRST_USER_FULL_NAME);

            assertThat(result).isEqualTo(new NotificationTemplate(
                    NotificationTemplateConstants.NEW_EVENT_THREAD_TITLE,
                    NotificationTemplateConstants.NEW_EVENT_THREAD_BODY
            ));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {
                InvalidInputConstants.EMPTY_VALUE,
                InvalidInputConstants.BLANK_VALUE,
                InvalidInputConstants.WHITESPACE_VALUE
        })
        @DisplayName("When creator full name is null or blank should throw IllegalArgumentException")
        void whenCreatorFullNameIsNullOrBlankShouldThrowIllegalArgumentException(String creatorFullName) {
            assertThatThrownBy(() -> notificationTemplateService.buildNewEventThread(creatorFullName))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Build thread reply template tests:")
    class BuildThreadReplyTests {

        @Test
        @DisplayName("When building thread reply template should return expected title and body")
        void whenBuildingThreadReplyTemplateShouldReturnExpectedTitleAndBody() {
            NotificationTemplate result = notificationTemplateService
                    .buildThreadReply(UserConstants.FIRST_USER_FULL_NAME);

            assertThat(result).isEqualTo(new NotificationTemplate(
                    NotificationTemplateConstants.THREAD_REPLY_TITLE,
                    NotificationTemplateConstants.THREAD_REPLY_BODY
            ));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {
                InvalidInputConstants.EMPTY_VALUE,
                InvalidInputConstants.BLANK_VALUE,
                InvalidInputConstants.WHITESPACE_VALUE
        })
        @DisplayName("When replier full name is null or blank should throw IllegalArgumentException")
        void whenReplierFullNameIsNullOrBlankShouldThrowIllegalArgumentException(String replierFullName) {
            assertThatThrownBy(() -> notificationTemplateService.buildThreadReply(replierFullName))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Build event update template tests:")
    class BuildEventUpdateTests {

        @Test
        @DisplayName("When building event update template should return expected title and body")
        void whenBuildingEventUpdateTemplateShouldReturnExpectedTitleAndBody() {
            NotificationTemplate result = notificationTemplateService
                    .buildEventUpdate(EventConstants.FIRST_EVENT_NAME);

            assertThat(result).isEqualTo(new NotificationTemplate(
                    NotificationTemplateConstants.EVENT_UPDATE_TITLE,
                    NotificationTemplateConstants.EVENT_UPDATE_BODY
            ));
        }

        @ParameterizedTest
        @NullSource
        @ValueSource(strings = {
                InvalidInputConstants.EMPTY_VALUE,
                InvalidInputConstants.BLANK_VALUE,
                InvalidInputConstants.WHITESPACE_VALUE
        })
        @DisplayName("When event name is null or blank should throw IllegalArgumentException")
        void whenEventNameIsNullOrBlankShouldThrowIllegalArgumentException(String eventName) {
            assertThatThrownBy(() -> notificationTemplateService.buildEventUpdate(eventName))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Build new event file template tests:")
    class BuildNewEventFileTests {

        @Test
        @DisplayName("When building new event file template should return expected title and body")
        void whenBuildingNewEventFileTemplateShouldReturnExpectedTitleAndBody() {
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
        @DisplayName("When uploader full name is null or blank should throw IllegalArgumentException")
        void whenUploaderFullNameIsNullOrBlankShouldThrowIllegalArgumentException(String uploaderFullName) {
            assertThatThrownBy(() -> notificationTemplateService.buildNewEventFile(uploaderFullName))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
