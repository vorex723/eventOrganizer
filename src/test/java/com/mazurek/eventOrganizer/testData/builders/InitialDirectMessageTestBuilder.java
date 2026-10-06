package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.conversation.ConversationCreationService.InitialDirectMessage;
import com.mazurek.eventOrganizer.conversation.dto.MessageDto;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.ConversationConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class InitialDirectMessageTestBuilder {
    private UUID conversationId = ConversationConstants.FIRST_CONVERSATION_ID;
    private MessageDto message;
    private boolean messageSet;

    public InitialDirectMessageTestBuilder conversationId(UUID conversationId) {
        this.conversationId = conversationId;
        return this;
    }

    public InitialDirectMessageTestBuilder message(MessageDto message) {
        this.message = message;
        this.messageSet = true;
        return this;
    }


    public InitialDirectMessage build() {
        return new InitialDirectMessage(
                conversationId,
                messageSet ? message : new MessageDtoTestBuilder().build()
        );
    }
}
