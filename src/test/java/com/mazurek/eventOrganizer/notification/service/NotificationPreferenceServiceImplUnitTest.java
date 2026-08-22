package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.exception.notification.InvalidNotificationPreferencesException;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.notification.domain.NotificationPreference;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.dto.NotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferenceDto;
import com.mazurek.eventOrganizer.notification.dto.UpdateNotificationPreferencesDto;
import com.mazurek.eventOrganizer.notification.repository.NotificationPreferenceRepository;
import com.mazurek.eventOrganizer.testData.builders.UserTestBuilder;
import com.mazurek.eventOrganizer.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.EMAIL;
import static com.mazurek.eventOrganizer.notification.domain.NotificationChannel.PUSH_ANDROID;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.CONVERSATION;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.EVENT;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.FILE;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.THREAD;
import static com.mazurek.eventOrganizer.notification.domain.NotificationResourceType.USER;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPreferenceServiceImpl unit tests:")
class NotificationPreferenceServiceImplUnitTest {

    @Mock
    private AuthenticationService authenticationService;
    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;
    @InjectMocks
    private NotificationPreferenceServiceImpl notificationPreferenceService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        currentUser = UserTestBuilder.firstUser().build();
    }

    @Nested
    @DisplayName("Get current user preferences tests:")
    class GetCurrentUserPreferencesTests {

        @Test
        @DisplayName("When no overrides exist should return complete default matrix in stable order")
        void whenNoOverridesExistShouldReturnCompleteDefaultMatrixInStableOrder() {
            when(authenticationService.getCurrentUserId()).thenReturn(FIRST_USER_ID);
            when(notificationPreferenceRepository.findByUserId(FIRST_USER_ID)).thenReturn(List.of());

            List<NotificationPreferenceDto> result = notificationPreferenceService
                    .getCurrentUserNotificationPreferences();

            assertThat(result).containsExactly(
                    preferenceDto(EVENT, PUSH_ANDROID, true),
                    preferenceDto(EVENT, EMAIL, true),
                    preferenceDto(THREAD, PUSH_ANDROID, true),
                    preferenceDto(THREAD, EMAIL, true),
                    preferenceDto(FILE, PUSH_ANDROID, true),
                    preferenceDto(FILE, EMAIL, true),
                    preferenceDto(CONVERSATION, PUSH_ANDROID, true),
                    preferenceDto(CONVERSATION, EMAIL, false),
                    preferenceDto(USER, PUSH_ANDROID, true),
                    preferenceDto(USER, EMAIL, true)
            );
        }

        @Test
        @DisplayName("When overrides exist should merge them into complete default matrix")
        void whenOverridesExistShouldMergeThemIntoCompleteDefaultMatrix() {
            when(authenticationService.getCurrentUserId()).thenReturn(FIRST_USER_ID);
            when(notificationPreferenceRepository.findByUserId(FIRST_USER_ID)).thenReturn(List.of(
                    preference(EVENT, PUSH_ANDROID, false),
                    preference(CONVERSATION, EMAIL, true)
            ));

            List<NotificationPreferenceDto> result = notificationPreferenceService
                    .getCurrentUserNotificationPreferences();

            assertThat(result)
                    .filteredOn(preference -> preference.resourceType() == EVENT
                            && preference.channel() == PUSH_ANDROID)
                    .containsExactly(preferenceDto(EVENT, PUSH_ANDROID, false));
            assertThat(result)
                    .filteredOn(preference -> preference.resourceType() == CONVERSATION
                            && preference.channel() == EMAIL)
                    .containsExactly(preferenceDto(CONVERSATION, EMAIL, true));
            assertThat(result).hasSize(
                    NotificationResourceType.values().length * NotificationChannel.values().length
            );
        }

    }

    @Nested
    @DisplayName("Resolve enabled channels tests:")
    class ResolveEnabledChannelsTests {

        @Test
        @DisplayName("When no overrides exist should return channels enabled by default")
        void whenNoOverridesExistShouldReturnChannelsEnabledByDefault() {
            when(notificationPreferenceRepository.findByUserIdAndResourceType(
                    FIRST_USER_ID,
                    CONVERSATION
            )).thenReturn(List.of());

            Set<NotificationChannel> result = notificationPreferenceService
                    .getEnabledExternalChannels(FIRST_USER_ID, CONVERSATION);

            assertThat(result).containsExactly(PUSH_ANDROID);
        }

        @Test
        @DisplayName("When overrides exist should return effective enabled channels")
        void whenOverridesExistShouldReturnEffectiveEnabledChannels() {
            when(notificationPreferenceRepository.findByUserIdAndResourceType(
                    FIRST_USER_ID,
                    CONVERSATION
            )).thenReturn(List.of(
                    preference(CONVERSATION, PUSH_ANDROID, false),
                    preference(CONVERSATION, EMAIL, true)
            ));

            Set<NotificationChannel> result = notificationPreferenceService
                    .getEnabledExternalChannels(FIRST_USER_ID, CONVERSATION);

            assertThat(result).containsExactly(EMAIL);
        }

        @Test
        @DisplayName("When checking one channel should resolve override before default")
        void whenCheckingOneChannelShouldResolveOverrideBeforeDefault() {
            when(notificationPreferenceRepository.findByUserIdAndResourceType(
                    FIRST_USER_ID,
                    CONVERSATION
            )).thenReturn(List.of(preference(CONVERSATION, EMAIL, true)));

            boolean result = notificationPreferenceService.isEnabled(
                    FIRST_USER_ID,
                    CONVERSATION,
                    EMAIL
            );

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("When checking one channel without override should use default")
        void whenCheckingOneChannelWithoutOverrideShouldUseDefault() {
            when(notificationPreferenceRepository.findByUserIdAndResourceType(
                    FIRST_USER_ID,
                    CONVERSATION
            )).thenReturn(List.of());

            boolean result = notificationPreferenceService.isEnabled(
                    FIRST_USER_ID,
                    CONVERSATION,
                    EMAIL
            );

            assertThat(result).isFalse();
        }

    }

    @Nested
    @DisplayName("Update current user preferences tests:")
    class UpdateCurrentUserPreferencesTests {

        @Test
        @DisplayName("When request contains only defaults should remove overrides and save no rows")
        void whenRequestContainsOnlyDefaultsShouldRemoveOverridesAndSaveNoRows() {
            when(authenticationService.getCurrentUser()).thenReturn(currentUser);

            notificationPreferenceService.updateCurrentUserNotificationPreferences(
                    new UpdateNotificationPreferencesDto(completeDefaultMatrix())
            );

            verify(notificationPreferenceRepository).deleteAllByUserId(FIRST_USER_ID);
            verify(notificationPreferenceRepository).saveAll(List.of());
        }

        @Test
        @DisplayName("When request differs from defaults should save only overrides")
        void whenRequestDiffersFromDefaultsShouldSaveOnlyOverrides() {
            when(authenticationService.getCurrentUser()).thenReturn(currentUser);
            List<UpdateNotificationPreferenceDto> requested = new ArrayList<>(completeDefaultMatrix());
            replace(requested, EVENT, PUSH_ANDROID, false);
            replace(requested, CONVERSATION, EMAIL, true);

            notificationPreferenceService.updateCurrentUserNotificationPreferences(
                    new UpdateNotificationPreferencesDto(requested)
            );

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<NotificationPreference>> captor = ArgumentCaptor.forClass(List.class);
            verify(notificationPreferenceRepository).saveAll(captor.capture());
            assertThat(captor.getValue())
                    .extracting(
                            NotificationPreference::getUserId,
                            NotificationPreference::getResourceType,
                            NotificationPreference::getChannel,
                            NotificationPreference::isEnabled
                    )
                    .containsExactly(
                            tuple(FIRST_USER_ID, EVENT, PUSH_ANDROID, false),
                            tuple(FIRST_USER_ID, CONVERSATION, EMAIL, true)
                    );
        }

        @Test
        @DisplayName("When preference matrix is incomplete should reject it before changing database")
        void whenPreferenceMatrixIsIncompleteShouldRejectItBeforeChangingDatabase() {
            when(authenticationService.getCurrentUser()).thenReturn(currentUser);
            List<UpdateNotificationPreferenceDto> incomplete = new ArrayList<>(completeDefaultMatrix());
            incomplete.remove(incomplete.size() - 1);

            assertThatThrownBy(() -> notificationPreferenceService.updateCurrentUserNotificationPreferences(
                    new UpdateNotificationPreferencesDto(incomplete)
            )).isInstanceOf(InvalidNotificationPreferencesException.class);

            verify(notificationPreferenceRepository, never()).deleteAllByUserId(FIRST_USER_ID);
            verify(notificationPreferenceRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        }

        @Test
        @DisplayName("When preference matrix contains duplicate should reject it before changing database")
        void whenPreferenceMatrixContainsDuplicateShouldRejectItBeforeChangingDatabase() {
            when(authenticationService.getCurrentUser()).thenReturn(currentUser);
            List<UpdateNotificationPreferenceDto> duplicated = new ArrayList<>(completeDefaultMatrix());
            duplicated.set(duplicated.size() - 1, duplicated.get(0));

            assertThatThrownBy(() -> notificationPreferenceService.updateCurrentUserNotificationPreferences(
                    new UpdateNotificationPreferencesDto(duplicated)
            )).isInstanceOf(InvalidNotificationPreferencesException.class);

            verifyNoInteractions(notificationPreferenceRepository);
        }

    }

    private static NotificationPreference preference(
            NotificationResourceType resourceType,
            NotificationChannel channel,
            boolean enabled
    ) {
        return NotificationPreference.builder()
                .userId(FIRST_USER_ID)
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
        return new NotificationPreferenceDto(resourceType, channel, enabled);
    }

    private static List<UpdateNotificationPreferenceDto> completeDefaultMatrix() {
        return Arrays.stream(NotificationResourceType.values())
                .flatMap(resourceType -> Arrays.stream(NotificationChannel.values())
                        .map(channel -> new UpdateNotificationPreferenceDto(
                                resourceType,
                                channel,
                                defaultEnabled(resourceType, channel)
                        )))
                .toList();
    }

    private static boolean defaultEnabled(
            NotificationResourceType resourceType,
            NotificationChannel channel
    ) {
        return channel == PUSH_ANDROID || resourceType != CONVERSATION;
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
                preferences.set(index, new UpdateNotificationPreferenceDto(
                        resourceType,
                        channel,
                        enabled
                ));
                return;
            }
        }

        throw new AssertionError("Preference combination not found in test matrix.");
    }
}
