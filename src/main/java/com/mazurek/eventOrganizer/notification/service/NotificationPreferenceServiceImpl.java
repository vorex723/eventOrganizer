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
import com.mazurek.eventOrganizer.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final AuthenticationService authenticationService;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    private static final Map<NotificationResourceType, Set<NotificationChannel>> DEFAULT_ENABLED_CHANNELS =
            Map.of(
                    NotificationResourceType.EVENT,
                    Set.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.PUSH_MOBILE,
                            NotificationChannel.PUSH_WEB
                    ),
                    NotificationResourceType.THREAD,
                    Set.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.PUSH_MOBILE,
                            NotificationChannel.PUSH_WEB
                            ),

                    NotificationResourceType.FILE,
                    Set.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.PUSH_MOBILE,
                            NotificationChannel.PUSH_WEB
                    ),

                    NotificationResourceType.CONVERSATION,
                    Set.of(
                            NotificationChannel.PUSH_MOBILE,
                            NotificationChannel.PUSH_WEB
                    ),

                    NotificationResourceType.USER,
                    Set.of(
                            NotificationChannel.EMAIL,
                            NotificationChannel.PUSH_MOBILE,
                            NotificationChannel.PUSH_WEB
                    )
            );

    private static final List<PreferenceKey> COMPLETE_PREFERENCE_MATRIX =
            Arrays.stream(NotificationResourceType.values())
                    .flatMap(resourceType ->
                            Arrays.stream(NotificationChannel.values())
                                    .map(channel -> new PreferenceKey(resourceType, channel))
                    )
                    .toList();

    private static final Set<PreferenceKey> EXPECTED_PREFERENCE_KEYS =
            Set.copyOf(COMPLETE_PREFERENCE_MATRIX);

    @Override
    @Transactional(readOnly = true)
    public List<NotificationPreferenceDto> getCurrentUserNotificationPreferences() {
        UUID userId = authenticationService.getCurrentUserId();
        Map<PreferenceKey, Boolean> overrides = toOverrideMap(
                notificationPreferenceRepository.findByUserId(userId)
        );

        return COMPLETE_PREFERENCE_MATRIX.stream()
                .map(key -> new NotificationPreferenceDto(
                        key.resourceType(),
                        key.channel(),
                        resolveEnabled(key, overrides)
                ))
                .toList();
    }

    @Transactional
    @Override
    public void updateCurrentUserNotificationPreferences(
            UpdateNotificationPreferencesDto request
    ) {
        User user = authenticationService.getCurrentUser();

        validateCompletePreferenceMatrix(request.preferences());

        List<NotificationPreference> desiredOverrides =
                request.preferences().stream()
                        .filter(preference ->
                                preference.enabled() != defaultEnabled(
                                        preference.resourceType(),
                                        preference.channel()
                                )
                        )
                        .map(preference ->
                                NotificationPreference.builder()
                                        .userId(user.getId())
                                        .resourceType(preference.resourceType())
                                        .channel(preference.channel())
                                        .enabled(preference.enabled())
                                        .build()
                        )
                        .toList();

        notificationPreferenceRepository.deleteAllByUserId(user.getId());
        notificationPreferenceRepository.saveAll(desiredOverrides);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<NotificationChannel> getEnabledExternalChannels(UUID userId, NotificationResourceType resourceType) {
        Map<PreferenceKey, Boolean> overrides = toOverrideMap(
                notificationPreferenceRepository.findByUserIdAndResourceType(userId, resourceType)
        );

        EnumSet<NotificationChannel> enabledChannels = Arrays.stream(NotificationChannel.values())
                .filter(channel -> resolveEnabled(
                        new PreferenceKey(resourceType, channel),
                        overrides
                ))
                .collect(Collectors.toCollection(
                        () -> EnumSet.noneOf(NotificationChannel.class)
                ));

        return Collections.unmodifiableSet(enabledChannels);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEnabled(UUID userId, NotificationResourceType resourceType, NotificationChannel channel) {
        Map<PreferenceKey, Boolean> overrides = toOverrideMap(
                notificationPreferenceRepository.findByUserIdAndResourceType(userId, resourceType)
        );

        return resolveEnabled(new PreferenceKey(resourceType, channel), overrides);
    }

    private boolean defaultEnabled(NotificationResourceType resourceType, NotificationChannel channel) {
        return DEFAULT_ENABLED_CHANNELS.get(resourceType).contains(channel);
    }

    private Map<PreferenceKey, Boolean> toOverrideMap(List<NotificationPreference> preferences) {
        return preferences.stream()
                .collect(Collectors.toUnmodifiableMap(
                        preference -> new PreferenceKey(
                                preference.getResourceType(),
                                preference.getChannel()
                        ),
                        NotificationPreference::isEnabled
                ));
    }

    private boolean resolveEnabled(
            PreferenceKey key,
            Map<PreferenceKey, Boolean> overrides
    ) {
        return overrides.getOrDefault(
                key,
                defaultEnabled(key.resourceType(), key.channel())
        );
    }

    private record PreferenceKey(
            NotificationResourceType resourceType,
            NotificationChannel channel
    ) {
    }

    private void validateCompletePreferenceMatrix(
            List<UpdateNotificationPreferenceDto> preferences
    ) {
        Set<PreferenceKey> receivedKeys = preferences.stream()
                .map(preference ->
                        new PreferenceKey(
                                preference.resourceType(),
                                preference.channel()
                        )
                )
                .collect(Collectors.toSet());

        boolean containsDuplicates =
                receivedKeys.size() != preferences.size();

        boolean incompleteOrUnexpected =
                !receivedKeys.equals(EXPECTED_PREFERENCE_KEYS);

        if (containsDuplicates || incompleteOrUnexpected) {
            throw new InvalidNotificationPreferencesException();
        }
    }
}
