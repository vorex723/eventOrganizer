package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.conversation.dto.SendDirectMessageDto;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.MessageConstants.FIRST_MESSAGE_CONTENT;
import static com.mazurek.eventOrganizer.testData.TestConstants.MessageConstants.SECOND_MESSAGE_CONTENT;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.FIRST_USER_ID;
import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants.SECOND_USER_ID;

public class SendDirectMessageDtoTestBuilder {

    private UUID recipientId = SECOND_USER_ID;
    private String content = FIRST_MESSAGE_CONTENT;

    public static SendDirectMessageDtoTestBuilder firstDirectMessage() {
        return new SendDirectMessageDtoTestBuilder();
    }

    public static SendDirectMessageDtoTestBuilder inverseDirectMessage() {
        return new SendDirectMessageDtoTestBuilder()
                .recipientId(FIRST_USER_ID)
                .content(SECOND_MESSAGE_CONTENT);
    }

    public SendDirectMessageDtoTestBuilder recipientId(UUID recipientId) {
        this.recipientId = recipientId;
        return this;
    }

    public SendDirectMessageDtoTestBuilder content(String content) {
        this.content = content;
        return this;
    }

    public SendDirectMessageDto build() {
        return SendDirectMessageDto.builder()
                .recipientId(recipientId)
                .content(content)
                .build();
    }
}
