package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.conversation.dto.MarkConversationReadDto;

import static com.mazurek.eventOrganizer.testData.TestConstants.MessageConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class MarkConversationReadDtoTestBuilder {
    private Long lastReadMessageId = MessageConstants.FIRST_MESSAGE_ID;

    public MarkConversationReadDtoTestBuilder lastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
        return this;
    }


    public MarkConversationReadDto build() {
        return new MarkConversationReadDto(
                lastReadMessageId
        );
    }
}
