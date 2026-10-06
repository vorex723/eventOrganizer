package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.NotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationPreferenceRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.NotificationPreferenceDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UpdateNotificationPreferenceDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UpdateNotificationPreferencesDtoTestBuilder;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.EMAIL;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_MOBILE;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_WEB;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.EVENT;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "app.firebase.enabled=false")
@ActiveProfiles("test")
@DisplayName("NotificationPreferenceAvailabilityIntegrationTest contracts:")
class NotificationPreferenceAvailabilityIntegrationTest {

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;
    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private DeletionService deletionService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        authHelper.setupSecurityContextForFirstUser();
        userId = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Test
    void whenFirebaseIsDisabledShouldPreservePushPreferences() {
        List<NotificationPreferenceDto> preferences = notificationPreferenceService
                .getCurrentUserNotificationPreferences();

        assertThat(preferences)
                .filteredOn(preference -> preference.resourceType() == EVENT)
                .containsExactly(
                        new NotificationPreferenceDtoTestBuilder()
                                .resourceType(EVENT)
                                .channel(PUSH_MOBILE)
                                .enabled(true)
                                .build(),
                        new NotificationPreferenceDtoTestBuilder()
                                .resourceType(EVENT)
                                .channel(PUSH_WEB)
                                .enabled(true)
                                .build(),
                        new NotificationPreferenceDtoTestBuilder()
                                .resourceType(EVENT)
                                .channel(EMAIL)
                                .enabled(false)
                                .build()
                );
        assertThat(notificationPreferenceService.getEnabledExternalChannels(userId, EVENT))
                .isEmpty();

        notificationPreferenceService.updateCurrentUserNotificationPreferences(
                new UpdateNotificationPreferencesDtoTestBuilder()
                        .version(0L)
                        .preferences(completeDefaultMatrix())
                        .build()
        );

        assertThat(notificationPreferenceRepository.findByUserId(userId)).isEmpty();
    }

    private static List<UpdateNotificationPreferenceDto> completeDefaultMatrix() {
        return Arrays.stream(NotificationResourceType.values())
                .flatMap(resourceType -> Arrays.stream(NotificationChannel.values())
                        .map(channel -> new UpdateNotificationPreferenceDtoTestBuilder()
                                .resourceType(resourceType)
                                .channel(channel)
                                .enabled(channel != EMAIL)
                                .build()))
                .toList();
    }
}
