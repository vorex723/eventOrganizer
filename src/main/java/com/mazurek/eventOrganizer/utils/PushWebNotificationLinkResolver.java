package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.config.properties.FrontendProperties;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PushWebNotificationLinkResolver {

    private final FrontendProperties frontendProperties;

    public String resolve(Notification notification) {
        if (notification == null
                || notification.getResourceType() == null
                || notification.getResourceId() == null) {
            return notificationsUrl();
        }

        return switch (notification.getResourceType()) {
            case EVENT -> resourceUrl("events", notification.getResourceId());
            case CONVERSATION -> resourceUrl("messages", notification.getResourceId());
            case USER -> resourceUrl("users", notification.getResourceId());
            case THREAD -> nestedEventResourceUrl(notification, "threads", null);
            case FILE -> nestedEventResourceUrl(notification, "files", notification.getResourceId());
        };
    }

    private String resourceUrl(String segment, UUID resourceId) {
        return baseUrlBuilder()
                .pathSegment(segment, resourceId.toString())
                .build()
                .encode()
                .toUriString();
    }

    private String nestedEventResourceUrl(
            Notification notification,
            String segment,
            UUID selectedFileId
    ) {
        if (notification.getParentResourceType() != NotificationResourceType.EVENT
                || notification.getParentResourceId() == null) {
            return notificationsUrl();
        }

        UriComponentsBuilder builder = baseUrlBuilder()
                .pathSegment(
                        "events",
                        notification.getParentResourceId().toString(),
                        segment
                );

        if (selectedFileId == null) {
            builder.pathSegment(notification.getResourceId().toString());
        } else {
            builder.queryParam("file", selectedFileId);
        }

        return builder.build().encode().toUriString();
    }

    private String notificationsUrl() {
        return baseUrlBuilder()
                .pathSegment("notifications")
                .build()
                .encode()
                .toUriString();
    }

    private UriComponentsBuilder baseUrlBuilder() {
        return UriComponentsBuilder.fromUri(frontendProperties.getUrl());
    }
}
