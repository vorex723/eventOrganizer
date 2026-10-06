package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.UserConstants;

/** Constructs data only; explicit overrides are passed through without repair. */
public class DirectConversationPairTestBuilder {
    private Conversation conversation;
    private boolean conversationSet;
    private UUID firstUserId = UserConstants.FIRST_USER_ID;
    private UUID secondUserId = UserConstants.SECOND_USER_ID;

    public DirectConversationPairTestBuilder conversation(Conversation conversation) {
        this.conversation = conversation;
        this.conversationSet = true;
        return this;
    }

    public DirectConversationPairTestBuilder firstUserId(UUID firstUserId) {
        this.firstUserId = firstUserId;
        return this;
    }

    public DirectConversationPairTestBuilder secondUserId(UUID secondUserId) {
        this.secondUserId = secondUserId;
        return this;
    }


    public DirectConversationPair build() {
        return DirectConversationPair.of(
                conversationSet ? conversation : ConversationTestBuilder.firstDirectConversation().build(),
                firstUserId,
                secondUserId
        );
    }
}
