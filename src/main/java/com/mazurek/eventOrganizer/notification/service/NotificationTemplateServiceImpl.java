package com.mazurek.eventOrganizer.notification.service;

import com.mazurek.eventOrganizer.notification.domain.NotificationTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private static final String PRIVATE_MESSAGE_TITLE = "New private message";
    private static final String PRIVATE_MESSAGE_BODY = "%s sent you a private message.";

    private static final String NEW_EVENT_THREAD_TITLE = "New event thread";
    private static final String NEW_EVENT_THREAD_BODY = "%s created a new thread in the event.";

    private static final String THREAD_REPLY_TITLE = "New thread reply";
    private static final String THREAD_REPLY_BODY = "%s replied to your thread.";

    private static final String EVENT_UPDATE_TITLE = "Event updated";
    private static final String EVENT_UPDATE_BODY = "The event \"%s\" has been updated.";

    private static final String NEW_EVENT_FILE_TITLE = "New event file";
    private static final String NEW_EVENT_FILE_BODY = "%s uploaded a new file to the event.";

    @Override
    public NotificationTemplate buildPrivateMessage(String senderFullName) {
        return buildTemplate(
                PRIVATE_MESSAGE_TITLE,
                PRIVATE_MESSAGE_BODY,
                senderFullName
        );
    }

    @Override
    public NotificationTemplate buildNewEventThread(String creatorFullName) {
        return buildTemplate(
                NEW_EVENT_THREAD_TITLE,
                NEW_EVENT_THREAD_BODY,
                creatorFullName
        );
    }

    @Override
    public NotificationTemplate buildThreadReply(String replierFullName) {
        return buildTemplate(
                THREAD_REPLY_TITLE,
                THREAD_REPLY_BODY,
                replierFullName
        );
    }

    @Override
    public NotificationTemplate buildEventUpdate(String eventName) {
        return buildTemplate(
                EVENT_UPDATE_TITLE,
                EVENT_UPDATE_BODY,
                eventName
        );
    }

    @Override
    public NotificationTemplate buildNewEventFile(String uploaderFullName) {
        return buildTemplate(
                NEW_EVENT_FILE_TITLE,
                NEW_EVENT_FILE_BODY,
                uploaderFullName
        );
    }

    private NotificationTemplate buildTemplate(
            String title,
            String bodyTemplate,
            String templateValue
    ) {
        if (templateValue == null || templateValue.isBlank()) {
            throw new IllegalArgumentException("Notification template value must not be blank.");
        }

        return new NotificationTemplate(title, bodyTemplate.formatted(templateValue));
    }
}
