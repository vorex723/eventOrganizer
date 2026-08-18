package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationDelivery;
import com.mazurek.eventOrganizer.notification.domain.NotificationResourceType;
import com.mazurek.eventOrganizer.notification.domain.NotificationTemplate;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class NotificationTestBuilder {

    private UUID id = NotificationConstants.PRIVATE_MESSAGE_NOTIFICATION_ID;
    private UUID recipientId = UserConstants.FIRST_USER_ID;
    private String title = NotificationConstants.PRIVATE_MESSAGE_TEMPLATE.title();
    private String body = NotificationConstants.PRIVATE_MESSAGE_TEMPLATE.body();
    private NotificationResourceType resourceType = NotificationResourceType.CONVERSATION;
    private UUID resourceId = ConversationConstants.FIRST_CONVERSATION_ID;
    private NotificationResourceType parentResourceType;
    private UUID parentResourceId;
    private Instant createdAt = NotificationConstants.PRIVATE_MESSAGE_NOTIFICATION_CREATED_AT;
    private Instant readAt;
    private Set<NotificationDelivery> deliveries = new HashSet<>();

    public static NotificationTestBuilder privateMessageNotification() {
        return new NotificationTestBuilder();
    }

    public static NotificationTestBuilder threadReplyNotification() {
        return new NotificationTestBuilder()
                .id(NotificationConstants.THREAD_REPLY_NOTIFICATION_ID)
                .template(NotificationConstants.THREAD_REPLY_TEMPLATE)
                .resourceType(NotificationResourceType.THREAD)
                .resourceId(ThreadConstants.FIRST_THREAD_ID)
                .parentResourceType(NotificationResourceType.EVENT)
                .parentResourceId(EventConstants.FIRST_EVENT_ID)
                .createdAt(NotificationConstants.THREAD_REPLY_NOTIFICATION_CREATED_AT);
    }

    public static NotificationTestBuilder eventUpdateNotification() {
        return new NotificationTestBuilder()
                .id(NotificationConstants.EVENT_UPDATE_NOTIFICATION_ID)
                .template(NotificationConstants.EVENT_UPDATE_TEMPLATE)
                .resourceType(NotificationResourceType.EVENT)
                .resourceId(EventConstants.FIRST_EVENT_ID)
                .createdAt(NotificationConstants.EVENT_UPDATE_NOTIFICATION_CREATED_AT);
    }

    public static NotificationTestBuilder newEventFileNotification() {
        return new NotificationTestBuilder()
                .id(NotificationConstants.NEW_EVENT_FILE_NOTIFICATION_ID)
                .template(NotificationConstants.NEW_EVENT_FILE_TEMPLATE)
                .resourceType(NotificationResourceType.FILE)
                .resourceId(FileConstants.FIRST_FILE_ID)
                .parentResourceType(NotificationResourceType.EVENT)
                .parentResourceId(EventConstants.FIRST_EVENT_ID)
                .createdAt(NotificationConstants.NEW_EVENT_FILE_NOTIFICATION_CREATED_AT);
    }

    public static NotificationTestBuilder newEventThreadNotification() {
        return new NotificationTestBuilder()
                .id(NotificationConstants.NEW_EVENT_THREAD_NOTIFICATION_ID)
                .template(NotificationConstants.NEW_EVENT_THREAD_TEMPLATE)
                .resourceType(NotificationResourceType.THREAD)
                .resourceId(ThreadConstants.FIRST_THREAD_ID)
                .parentResourceType(NotificationResourceType.EVENT)
                .parentResourceId(EventConstants.FIRST_EVENT_ID)
                .createdAt(NotificationConstants.NEW_EVENT_THREAD_NOTIFICATION_CREATED_AT);
    }

    public NotificationTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public NotificationTestBuilder recipientId(UUID recipientId) {
        this.recipientId = recipientId;
        return this;
    }

    public NotificationTestBuilder template(NotificationTemplate template) {
        this.title = template.title();
        this.body = template.body();
        return this;
    }

    public NotificationTestBuilder title(String title) {
        this.title = title;
        return this;
    }

    public NotificationTestBuilder body(String body) {
        this.body = body;
        return this;
    }

    public NotificationTestBuilder resourceType(NotificationResourceType resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public NotificationTestBuilder resourceId(UUID resourceId) {
        this.resourceId = resourceId;
        return this;
    }

    public NotificationTestBuilder parentResourceType(NotificationResourceType parentResourceType) {
        this.parentResourceType = parentResourceType;
        return this;
    }

    public NotificationTestBuilder parentResourceId(UUID parentResourceId) {
        this.parentResourceId = parentResourceId;
        return this;
    }

    public NotificationTestBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public NotificationTestBuilder readAt(Instant readAt) {
        this.readAt = readAt;
        return this;
    }

    public NotificationTestBuilder deliveries(Set<NotificationDelivery> deliveries) {
        this.deliveries = deliveries;
        return this;
    }

    public Notification build() {
        return Notification.builder()
                .id(id)
                .recipientId(recipientId)
                .title(title)
                .body(body)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .parentResourceType(parentResourceType)
                .parentResourceId(parentResourceId)
                .createdAt(createdAt)
                .readAt(readAt)
                .deliveries(deliveries)
                .build();
    }
}
