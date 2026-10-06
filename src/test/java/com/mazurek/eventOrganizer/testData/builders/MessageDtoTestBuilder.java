package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.conversation.dto.MessageDto;
import java.time.Instant;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.MessageConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.MessageFixtureConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class MessageDtoTestBuilder {
    private Long id = MessageConstants.FIRST_MESSAGE_ID;
    private UUID senderId = UserConstants.FIRST_USER_ID;
    private String senderName = UserConstants.FIRST_USER_FULL_NAME;
    private Instant sentDate = TimeConstants.NOW;
    private String content = MessageConstants.FIRST_MESSAGE_CONTENT;
    private boolean contentUnavailable = MessageFixtureConstants.CONTENT_UNAVAILABLE;

    public MessageDtoTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public MessageDtoTestBuilder senderId(UUID senderId) {
        this.senderId = senderId;
        return this;
    }

    public MessageDtoTestBuilder senderName(String senderName) {
        this.senderName = senderName;
        return this;
    }

    public MessageDtoTestBuilder sentDate(Instant sentDate) {
        this.sentDate = sentDate;
        return this;
    }

    public MessageDtoTestBuilder content(String content) {
        this.content = content;
        return this;
    }

    public MessageDtoTestBuilder contentUnavailable(boolean contentUnavailable) {
        this.contentUnavailable = contentUnavailable;
        return this;
    }


    public MessageDto build() {
        return new MessageDto(
                id,
                senderId,
                senderName,
                sentDate,
                content,
                contentUnavailable
        );
    }
}
