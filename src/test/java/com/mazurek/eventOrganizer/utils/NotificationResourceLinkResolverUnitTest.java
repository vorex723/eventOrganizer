package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.config.properties.FrontendProperties;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.testData.builders.NotificationTestBuilder;
import com.mazurek.eventOrganizer.testData.builders.FrontendPropertiesTestBuilder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NotificationResourceLinkResolverUnitTest contracts:")
class NotificationResourceLinkResolverUnitTest {

    private static final UUID RESOURCE_ID = com.mazurek.eventOrganizer.testData.TestConstants.ConversationConstants.FIRST_CONVERSATION_ID;
    private static final UUID EVENT_ID = com.mazurek.eventOrganizer.testData.TestConstants.EventConstants.FIRST_EVENT_ID;

    private NotificationResourceLinkResolver resolver;

    @BeforeEach
    void setUp() {
        FrontendProperties properties = new FrontendPropertiesTestBuilder()
                .url(URI.create("https://localhost:5173"))
                .build();
        resolver = new NotificationResourceLinkResolver(properties);
    }

    @Test
    void whenReferenceIsDirectShouldResolveResourceLink() {
        assertThat(resolve(NotificationResourceType.EVENT))
                .isEqualTo("https://localhost:5173/events/" + RESOURCE_ID);
        assertThat(resolve(NotificationResourceType.CONVERSATION))
                .isEqualTo("https://localhost:5173/messages/" + RESOURCE_ID);
        assertThat(resolve(NotificationResourceType.USER))
                .isEqualTo("https://localhost:5173/users/" + RESOURCE_ID);
    }

    @Test
    void whenReferenceIsEventThreadShouldResolveNestedLink() {
        Notification notification = notification(NotificationResourceType.THREAD)
                .parentResourceType(NotificationResourceType.EVENT)
                .parentResourceId(EVENT_ID)
                .build();

        assertThat(resolver.resolve(notification))
                .isEqualTo("https://localhost:5173/events/" + EVENT_ID + "/threads/" + RESOURCE_ID);
    }

    @Test
    void whenReferenceIsEventFileShouldResolveNestedLink() {
        Notification notification = notification(NotificationResourceType.FILE)
                .parentResourceType(NotificationResourceType.EVENT)
                .parentResourceId(EVENT_ID)
                .build();

        assertThat(resolver.resolve(notification))
                .isEqualTo("https://localhost:5173/events/" + EVENT_ID + "/files?file=" + RESOURCE_ID);
    }

    @Test
    void whenNestedReferenceIsInvalidShouldFallBackToNotifications() {
        Notification notification = notification(NotificationResourceType.THREAD).build();

        assertThat(resolver.resolve(notification))
                .isEqualTo("https://localhost:5173/notifications");
    }

    @Test
    void whenBaseUrlHasTrailingSlashShouldNormalizeLink() {
        FrontendProperties properties = new FrontendPropertiesTestBuilder()
                .url(URI.create("https://localhost:5173/"))
                .build();
        NotificationResourceLinkResolver trailingSlashResolver =
                new NotificationResourceLinkResolver(properties);

        assertThat(trailingSlashResolver.resolve(notification(NotificationResourceType.EVENT).build()))
                .isEqualTo("https://localhost:5173/events/" + RESOURCE_ID);
    }

    private String resolve(NotificationResourceType resourceType) {
        return resolver.resolve(notification(resourceType).build());
    }

    private NotificationTestBuilder notification(NotificationResourceType resourceType) {
        return new NotificationTestBuilder().id(null)
                .recipientId(null)
                .title(null)
                .body(null)
                .createdAt(null)
                .resourceType(resourceType)
                .resourceId(RESOURCE_ID);
    }
}
