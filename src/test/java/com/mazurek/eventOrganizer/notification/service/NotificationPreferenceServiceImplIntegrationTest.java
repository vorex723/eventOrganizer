package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.DeletionService;
import com.mazurek.eventOrganizer.exception.notification.InvalidNotificationPreferencesException;
import com.mazurek.eventOrganizer.exception.user.UserBannedException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationPreference;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.NotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationPreferenceRepository;
import com.mazurek.eventOrganizer.testData.AuthHelper;
import com.mazurek.eventOrganizer.testData.builders.NotificationPreferenceDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.NotificationPreferenceTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UpdateNotificationPreferenceDtoTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.UpdateNotificationPreferencesDtoTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.EMAIL;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_MOBILE;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_WEB;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.CONVERSATION;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.EVENT;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.FILE;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.THREAD;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.USER;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_EMAIL;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.SECOND_USER_EMAIL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("NotificationPreferenceService integration tests:")
class NotificationPreferenceServiceImplIntegrationTest {

    @Autowired
    private NotificationPreferenceService notificationPreferenceService;
    @Autowired
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthHelper authHelper;
    @Autowired
    private com.mazurek.eventOrganizer.testData.TestPersistenceQueries persistenceQueries;
    @Autowired
    private DeletionService deletionService;

    private UUID firstUserId;
    private UUID secondUserId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
        authHelper.setupRolesAndUsers();
        authHelper.setupSecurityContextForFirstUser();

        firstUserId = userRepository.findByIgnoreCaseEmail(FIRST_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
        secondUserId = userRepository.findByIgnoreCaseEmail(SECOND_USER_EMAIL)
                .orElseThrow(UserNotFoundException::new)
                .getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        deletionService.deleteAllSafe();
    }

    @Nested
    @DisplayName("Read preferences tests:")
    class ReadPreferencesTests {

        @Test
        @DisplayName("When no overrides exist should return the complete default matrix")
        void whenNoOverridesExistShouldReturnCompleteDefaultMatrix() {
            var beforeOperation = persistenceQueries.notificationState();

            List<NotificationPreferenceDto> result = notificationPreferenceService
                    .getCurrentUserNotificationPreferences();

            assertThat(result).containsExactly(
                    preferenceDto(EVENT, PUSH_MOBILE, true),
                    preferenceDto(EVENT, PUSH_WEB, true),
                    preferenceDto(EVENT, EMAIL, false),
                    preferenceDto(THREAD, PUSH_MOBILE, true),
                    preferenceDto(THREAD, PUSH_WEB, true),
                    preferenceDto(THREAD, EMAIL, false),
                    preferenceDto(FILE, PUSH_MOBILE, true),
                    preferenceDto(FILE, PUSH_WEB, true),
                    preferenceDto(FILE, EMAIL, false),
                    preferenceDto(CONVERSATION, PUSH_MOBILE, true),
                    preferenceDto(CONVERSATION, PUSH_WEB, true),
                    preferenceDto(CONVERSATION, EMAIL, false),
                    preferenceDto(USER, PUSH_MOBILE, true),
                    preferenceDto(USER, PUSH_WEB, true),
                    preferenceDto(USER, EMAIL, false)
            );
            assertThat(persistenceQueries.notificationState())
                    .as("Read/rejection must preserve committed notification state")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When overrides exist should resolve effective external channels")
        void whenOverridesExistShouldResolveEffectiveExternalChannels() {
            notificationPreferenceRepository.saveAllAndFlush(List.of(
                    preference(CONVERSATION, PUSH_MOBILE, false),
                    preference(CONVERSATION, EMAIL, true)
            ));

            var beforeOperation = persistenceQueries.notificationState();

            Set<NotificationChannel> enabledChannels = notificationPreferenceService
                    .getEnabledExternalChannels(firstUserId, CONVERSATION);

            assertThat(enabledChannels).containsExactly(PUSH_WEB);
            assertThat(notificationPreferenceService.isEnabled(
                    firstUserId,
                    CONVERSATION,
                    PUSH_MOBILE
            )).isFalse();
            assertThat(notificationPreferenceService.isEnabled(
                    firstUserId,
                    CONVERSATION,
                    PUSH_WEB
            )).isTrue();
            assertThat(notificationPreferenceService.isEnabled(
                    firstUserId,
                    CONVERSATION,
                    EMAIL
            )).isTrue();
            assertThat(persistenceQueries.notificationState())
                    .as("Read/rejection must preserve committed notification state")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When every external channel is disabled should return an empty set")
        void whenEveryExternalChannelIsDisabledShouldReturnEmptySet() {
            notificationPreferenceRepository.saveAllAndFlush(List.of(
                    preference(CONVERSATION, PUSH_MOBILE, false),
                    preference(CONVERSATION, PUSH_WEB, false)
            ));

            var beforeOperation = persistenceQueries.notificationState();

            Set<NotificationChannel> enabledChannels = notificationPreferenceService
                    .getEnabledExternalChannels(firstUserId, CONVERSATION);

            assertThat(enabledChannels).isEmpty();
            assertThat(persistenceQueries.notificationState())
                    .as("Read/rejection must preserve committed notification state")
                    .isEqualTo(beforeOperation);
        }

    }

    @Nested
    @DisplayName("Update preferences tests:")
    class UpdatePreferencesTests {

        @Test
        @DisplayName("When preferences differ from defaults should persist only overrides")
        void whenPreferencesDifferFromDefaultsShouldPersistOnlyOverrides() {
            List<UpdateNotificationPreferenceDto> requested = mutableDefaultMatrix();
            replace(requested, EVENT, PUSH_WEB, false);

            notificationPreferenceService.updateCurrentUserNotificationPreferences(
                    new UpdateNotificationPreferencesDtoTestBuilder()
                            .version(0L)
                            .preferences(requested)
                            .build()
            );

            assertThat(notificationPreferenceRepository.findByUserId(firstUserId))
                    .extracting(
                            NotificationPreference::getResourceType,
                            NotificationPreference::getChannel,
                            NotificationPreference::isEnabled
                    )
                    .containsExactlyInAnyOrder(
                            tuple(EVENT, PUSH_WEB, false)
                    );

            assertThat(notificationPreferenceService.getCurrentUserNotificationPreferences())
                    .filteredOn(preference ->
                            preference.resourceType() == EVENT
                                    && preference.channel() == PUSH_WEB
                    )
                    .containsExactly(preferenceDto(EVENT, PUSH_WEB, false));
            assertThat(notificationPreferenceService.getCurrentUserNotificationPreferences())
                    .filteredOn(preference ->
                            preference.resourceType() == CONVERSATION
                                    && preference.channel() == EMAIL
                    )
                    .containsExactly(preferenceDto(CONVERSATION, EMAIL, false));
        }

        @Test
        @DisplayName("When notification email is unavailable should preserve enabled preference")
        void whenNotificationEmailIsUnavailableShouldPreserveEnabledPreference() {
            List<UpdateNotificationPreferenceDto> requested = mutableDefaultMatrix();
            replace(requested, CONVERSATION, EMAIL, true);

            notificationPreferenceService.updateCurrentUserNotificationPreferences(
                    new UpdateNotificationPreferencesDtoTestBuilder()
                            .version(0L)
                            .preferences(requested)
                            .build()
            );

            assertThat(notificationPreferenceRepository.findByUserId(firstUserId))
                    .extracting(
                            NotificationPreference::getResourceType,
                            NotificationPreference::getChannel,
                            NotificationPreference::isEnabled
                    )
                    .containsExactly(tuple(CONVERSATION, EMAIL, true));
        }

        @Test
        @DisplayName("When preferences are updated again should replace the previous overrides")
        void whenPreferencesAreUpdatedAgainShouldReplacePreviousOverrides() {
            List<UpdateNotificationPreferenceDto> firstRequest = mutableDefaultMatrix();
            replace(firstRequest, EVENT, PUSH_WEB, false);
            notificationPreferenceService.updateCurrentUserNotificationPreferences(
                    new UpdateNotificationPreferencesDtoTestBuilder()
                            .version(0L)
                            .preferences(firstRequest)
                            .build()
            );

            List<UpdateNotificationPreferenceDto> secondRequest = mutableDefaultMatrix();
            replace(secondRequest, EVENT, PUSH_WEB, false);
            notificationPreferenceService.updateCurrentUserNotificationPreferences(
                    new UpdateNotificationPreferencesDtoTestBuilder()
                            .version(1L)
                            .preferences(secondRequest)
                            .build()
            );

            assertThat(notificationPreferenceRepository.findByUserId(firstUserId))
                    .extracting(
                            NotificationPreference::getResourceType,
                            NotificationPreference::getChannel,
                            NotificationPreference::isEnabled
                    )
                    .containsExactlyInAnyOrder(tuple(EVENT, PUSH_WEB, false));
        }

        @Test
        @DisplayName("When all submitted preferences match defaults should remove every override")
        void whenAllSubmittedPreferencesMatchDefaultsShouldRemoveEveryOverride() {
            notificationPreferenceRepository.saveAndFlush(
                    preference(CONVERSATION, EMAIL, true)
            );

            notificationPreferenceService.updateCurrentUserNotificationPreferences(
                    new UpdateNotificationPreferencesDtoTestBuilder()
                            .version(0L)
                            .preferences(completeDefaultMatrix())
                            .build()
            );

            assertThat(notificationPreferenceRepository.findByUserId(firstUserId)).isEmpty();
        }

        @Test
        @DisplayName("When updating preferences should preserve another user's overrides")
        void whenUpdatingPreferencesShouldPreserveAnotherUsersOverrides() {
            notificationPreferenceRepository.saveAndFlush(
                    preference(secondUserId, CONVERSATION, EMAIL, true)
            );
            List<UpdateNotificationPreferenceDto> requested = mutableDefaultMatrix();
            replace(requested, EVENT, PUSH_WEB, false);

            notificationPreferenceService.updateCurrentUserNotificationPreferences(
                    new UpdateNotificationPreferencesDtoTestBuilder()
                            .version(0L)
                            .preferences(requested)
                            .build()
            );

            assertThat(notificationPreferenceRepository.findByUserId(firstUserId))
                    .extracting(
                            NotificationPreference::getResourceType,
                            NotificationPreference::getChannel,
                            NotificationPreference::isEnabled
                    )
                    .containsExactly(tuple(EVENT, PUSH_WEB, false));
            assertThat(notificationPreferenceRepository.findByUserId(secondUserId))
                    .extracting(
                            NotificationPreference::getResourceType,
                            NotificationPreference::getChannel,
                            NotificationPreference::isEnabled
                    )
                    .containsExactly(tuple(CONVERSATION, EMAIL, true));
        }

        @Test
        @DisplayName("When submitted matrix is incomplete should preserve existing overrides")
        void whenSubmittedMatrixIsIncompleteShouldPreserveExistingOverrides() {
            long versionBefore = userRepository.findById(firstUserId)
                    .orElseThrow(UserNotFoundException::new).getNotificationPreferencesVersion();
            notificationPreferenceRepository.saveAndFlush(
                    preference(CONVERSATION, EMAIL, true)
            );
            List<UpdateNotificationPreferenceDto> incomplete = mutableDefaultMatrix();
            incomplete.removeIf(preference ->
                    preference.resourceType() == USER && preference.channel() == PUSH_WEB
            );

            var beforeOperation = persistenceQueries.notificationState();

            assertThatThrownBy(() -> notificationPreferenceService
                    .updateCurrentUserNotificationPreferences(
                            new UpdateNotificationPreferencesDtoTestBuilder()
                                    .version(0L)
                                    .preferences(incomplete)
                                    .build()
                    ))
                    .isInstanceOf(InvalidNotificationPreferencesException.class);

            assertThat(notificationPreferenceRepository.findByUserId(firstUserId))
                    .extracting(
                            NotificationPreference::getResourceType,
                            NotificationPreference::getChannel,
                            NotificationPreference::isEnabled
                    )
                    .containsExactly(tuple(CONVERSATION, EMAIL, true));

            assertThat(userRepository.findById(firstUserId).orElseThrow(UserNotFoundException::new)
                    .getNotificationPreferencesVersion()).isEqualTo(versionBefore);
            assertThat(persistenceQueries.notificationState())
                    .as("Read/rejection must preserve committed notification state")
                    .isEqualTo(beforeOperation);
        }

        @Test
        @DisplayName("When current user is banned should allow reads but reject updates")
        void whenCurrentUserIsBannedShouldAllowReadsButRejectUpdates() {
            notificationPreferenceRepository.saveAndFlush(
                    preference(CONVERSATION, EMAIL, true)
            );
            User user = userRepository.findById(firstUserId)
                    .orElseThrow(UserNotFoundException::new);
            user.setBanned(true);
            userRepository.saveAndFlush(user);
            long versionBefore = user.getNotificationPreferencesVersion();

            var beforeOperation = persistenceQueries.notificationState();

            assertThat(notificationPreferenceService.getCurrentUserNotificationPreferences())
                    .filteredOn(preference ->
                            preference.resourceType() == CONVERSATION
                                    && preference.channel() == EMAIL
                    )
                    .containsExactly(preferenceDto(CONVERSATION, EMAIL, true));

            assertThatThrownBy(() -> notificationPreferenceService
                    .updateCurrentUserNotificationPreferences(
                            new UpdateNotificationPreferencesDtoTestBuilder()
                                    .version(0L)
                                    .preferences(completeDefaultMatrix())
                                    .build()
                    ))
                    .isInstanceOf(UserBannedException.class);

            assertThat(notificationPreferenceRepository.findByUserId(firstUserId))
                    .extracting(
                            NotificationPreference::getResourceType,
                            NotificationPreference::getChannel,
                            NotificationPreference::isEnabled
                    )
                    .containsExactly(tuple(CONVERSATION, EMAIL, true));

            assertThat(userRepository.findById(firstUserId).orElseThrow(UserNotFoundException::new)
                    .getNotificationPreferencesVersion()).isEqualTo(versionBefore);
            assertThat(persistenceQueries.notificationState())
                    .as("Read/rejection must preserve committed notification state")
                    .isEqualTo(beforeOperation);
        }

    }

    private NotificationPreference preference(
            NotificationResourceType resourceType,
            NotificationChannel channel,
            boolean enabled
    ) {
        return preference(firstUserId, resourceType, channel, enabled);
    }

    private NotificationPreference preference(
            UUID userId,
            NotificationResourceType resourceType,
            NotificationChannel channel,
            boolean enabled
    ) {
        return new NotificationPreferenceTestBuilder().id(null)
                .userId(userId)
                .resourceType(resourceType)
                .channel(channel)
                .enabled(enabled)
                .build();
    }

    private static NotificationPreferenceDto preferenceDto(
            NotificationResourceType resourceType,
            NotificationChannel channel,
            boolean enabled
    ) {
        return new NotificationPreferenceDtoTestBuilder()
                .resourceType(resourceType)
                .channel(channel)
                .enabled(enabled)
                .build();
    }

    private static List<UpdateNotificationPreferenceDto> completeDefaultMatrix() {
        return Arrays.stream(NotificationResourceType.values())
                .flatMap(resourceType -> Arrays.stream(NotificationChannel.values())
                        .map(channel -> new UpdateNotificationPreferenceDtoTestBuilder()
                                .resourceType(resourceType)
                                .channel(channel)
                                .enabled(defaultEnabled(resourceType, channel))
                                .build()))
                .toList();
    }

    private static List<UpdateNotificationPreferenceDto> mutableDefaultMatrix() {
        return new ArrayList<>(completeDefaultMatrix());
    }

    private static boolean defaultEnabled(
            NotificationResourceType resourceType,
            NotificationChannel channel
    ) {
        return channel != EMAIL;
    }

    private static void replace(
            List<UpdateNotificationPreferenceDto> preferences,
            NotificationResourceType resourceType,
            NotificationChannel channel,
            boolean enabled
    ) {
        for (int index = 0; index < preferences.size(); index++) {
            UpdateNotificationPreferenceDto preference = preferences.get(index);
            if (preference.resourceType() == resourceType && preference.channel() == channel) {
                preferences.set(index, new UpdateNotificationPreferenceDtoTestBuilder()
                        .resourceType(resourceType)
                        .channel(channel)
                        .enabled(enabled)
                        .build());
                return;
            }
        }

        throw new AssertionError("Preference combination not found in test matrix.");
    }
}
