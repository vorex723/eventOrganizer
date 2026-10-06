package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.auth.email.AuthEmailType;
import com.mazurek.eventOrganizer.auth.email.LocalAuthEmail;
import java.time.Instant;

import static com.mazurek.eventOrganizer.testData.TestConstants.DeliveryFixtureConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.SeedReportConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class LocalAuthEmailTestBuilder {
    private AuthEmailType type = DeliveryFixtureConstants.AUTH_TYPE;
    private String recipientEmail = UserConstants.FIRST_USER_EMAIL;
    private String link = SeedReportConstants.AUTH_LINK;
    private Instant sentAt = TimeConstants.NOW;

    public LocalAuthEmailTestBuilder type(AuthEmailType type) {
        this.type = type;
        return this;
    }

    public LocalAuthEmailTestBuilder recipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
        return this;
    }

    public LocalAuthEmailTestBuilder link(String link) {
        this.link = link;
        return this;
    }

    public LocalAuthEmailTestBuilder sentAt(Instant sentAt) {
        this.sentAt = sentAt;
        return this;
    }


    public LocalAuthEmail build() {
        return new LocalAuthEmail(
                type,
                recipientEmail,
                link,
                sentAt
        );
    }
}
