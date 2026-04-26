package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.user.User;

import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

/**
 * Builder for creating Conversation test objects with sensible defaults.
 */
public class ConversationTestBuilder {

    private UUID id = ConversationConstants.FIRST_CONVERSATION_ID;
    private User firstParticipant = UserTestBuilder.firstUser().build();
    private User secondParticipant = UserTestBuilder.secondUser().build();

    public static ConversationTestBuilder firstConversation() {
        return new ConversationTestBuilder()
                .id(ConversationConstants.FIRST_CONVERSATION_ID)
                .participants(
                        UserTestBuilder.firstUser().build(),
                        UserTestBuilder.secondUser().build()
                );
    }

    public static ConversationTestBuilder secondConversation() {
        return new ConversationTestBuilder()
                .id(ConversationConstants.SECOND_CONVERSATION_ID)
                .participants(
                        UserTestBuilder.firstUser().build(),
                        UserTestBuilder.thirdUser().build()
                );
    }

    public ConversationTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public ConversationTestBuilder participants(User first, User second) {
        this.firstParticipant = first;
        this.secondParticipant = second;
        return this;
    }

    public Conversation build() {
        Conversation conversation = Conversation.builder()
                .id(id)
                .build();

        // Add participants using constructor logic or manually
        conversation.addParticipant(firstParticipant);
        conversation.addParticipant(secondParticipant);

        return conversation;
    }
}