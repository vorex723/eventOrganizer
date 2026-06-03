package com.mazurek.eventOrganizer.testData.builders.dto;

import com.mazurek.eventOrganizer.conversation.dto.SendConversationMessageDto;

import static com.mazurek.eventOrganizer.testData.TestConstants.MessageConstants.FIRST_MESSAGE_CONTENT;
import static com.mazurek.eventOrganizer.testData.TestConstants.MessageConstants.SECOND_MESSAGE_CONTENT;

public class SendConversationMessageDtoTestBuilder {

    private String content = FIRST_MESSAGE_CONTENT;

    public static SendConversationMessageDtoTestBuilder firstConversationMessage() {
        return new SendConversationMessageDtoTestBuilder();
    }

    public static SendConversationMessageDtoTestBuilder secondConversationMessage() {
        return new SendConversationMessageDtoTestBuilder()
                .content(SECOND_MESSAGE_CONTENT);
    }

    public SendConversationMessageDtoTestBuilder content(String content) {
        this.content = content;
        return this;
    }

    public SendConversationMessageDto build() {
        return new SendConversationMessageDto(content);
    }
}
