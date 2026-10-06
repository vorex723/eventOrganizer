package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.notification.domain.NotificationTemplate;

import static com.mazurek.eventOrganizer.testData.TestConstants.NotificationTemplateConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationTemplateTestBuilder {
    private String title = NotificationTemplateConstants.EVENT_UPDATE_TITLE;
    private String body = NotificationTemplateConstants.EVENT_UPDATE_BODY;

    public NotificationTemplateTestBuilder title(String title) {
        this.title = title;
        return this;
    }

    public NotificationTemplateTestBuilder body(String body) {
        this.body = body;
        return this;
    }


    public NotificationTemplate build() {
        return new NotificationTemplate(
                title,
                body
        );
    }
}
