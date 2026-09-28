package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.config.properties.FrontendProperties;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PushWebNotificationLinkResolverUnitTest {

    private static final UUID RESOURCE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID EVENT_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");

    private PushWebNotificationLinkResolver resolver;

    @BeforeEach
    void setUp() {
        FrontendProperties properties = new FrontendProperties();
        properties.setUrl(URI.create("https://localhost:5173"));
        resolver = new PushWebNotificationLinkResolver(properties);
    }

    @Test
    void resolvesDirectResourceLinks() {
        assertThat(resolve(NotificationResourceType.EVENT))
                .isEqualTo("https://localhost:5173/events/" + RESOURCE_ID);
        assertThat(resolve(NotificationResourceType.CONVERSATION))
                .isEqualTo("https://localhost:5173/messages/" + RESOURCE_ID);
        assertThat(resolve(NotificationResourceType.USER))
                .isEqualTo("https://localhost:5173/users/" + RESOURCE_ID);
    }

    @Test
    void resolvesEventThreadLink() {
        Notification notification = notification(NotificationResourceType.THREAD)
                .parentResourceType(NotificationResourceType.EVENT)
                .parentResourceId(EVENT_ID)
                .build();

        assertThat(resolver.resolve(notification))
                .isEqualTo("https://localhost:5173/events/" + EVENT_ID + "/threads/" + RESOURCE_ID);
    }

    @Test
    void resolvesEventFileLink() {
        Notification notification = notification(NotificationResourceType.FILE)
                .parentResourceType(NotificationResourceType.EVENT)
                .parentResourceId(EVENT_ID)
                .build();

        assertThat(resolver.resolve(notification))
                .isEqualTo("https://localhost:5173/events/" + EVENT_ID + "/files?file=" + RESOURCE_ID);
    }

    @Test
    void fallsBackToNotificationsForInvalidNestedReference() {
        Notification notification = notification(NotificationResourceType.THREAD).build();

        assertThat(resolver.resolve(notification))
                .isEqualTo("https://localhost:5173/notifications");
    }

    @Test
    void normalizesTrailingSlashInBaseUrl() {
        FrontendProperties properties = new FrontendProperties();
        properties.setUrl(URI.create("https://localhost:5173/"));
        PushWebNotificationLinkResolver trailingSlashResolver =
                new PushWebNotificationLinkResolver(properties);

        assertThat(trailingSlashResolver.resolve(notification(NotificationResourceType.EVENT).build()))
                .isEqualTo("https://localhost:5173/events/" + RESOURCE_ID);
    }

    private String resolve(NotificationResourceType resourceType) {
        return resolver.resolve(notification(resourceType).build());
    }

    private Notification.NotificationBuilder notification(NotificationResourceType resourceType) {
        return Notification.builder()
                .resourceType(resourceType)
                .resourceId(RESOURCE_ID);
    }
}
