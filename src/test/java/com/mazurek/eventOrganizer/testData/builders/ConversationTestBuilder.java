package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.ConversationType;
import com.mazurek.eventOrganizer.user.User;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

/**
 * Builder for creating Conversation test objects with sensible defaults.
 */
public class ConversationTestBuilder {

    private UUID id = ConversationConstants.FIRST_CONVERSATION_ID;
    private ConversationType type = ConversationConstants.DEFAULT_TYPE;
    private Instant createdAt = TimeConstants.NOW;
    private Instant lastActiveAt = TimeConstants.NOW;
    private String name = null ;
    private User firstParticipant = UserTestBuilder.firstUser().build();
    private User secondParticipant = UserTestBuilder.secondUser().build();

    public static ConversationTestBuilder firstDirectConversation() {
        return new ConversationTestBuilder()
                .id(ConversationConstants.FIRST_CONVERSATION_ID)
                .type(ConversationConstants.DEFAULT_TYPE)
                .participants(
                        UserTestBuilder.firstUser().build(),
                        UserTestBuilder.secondUser().build()
                );
    }

    public static ConversationTestBuilder secondDirectConversation() {
        return new ConversationTestBuilder()
                .id(ConversationConstants.SECOND_CONVERSATION_ID)
                .type(ConversationConstants.DEFAULT_TYPE)
                .participants(
                        UserTestBuilder.firstUser().build(),
                        UserTestBuilder.thirdUser().build()
                );
    }

    public ConversationTestBuilder id(UUID id) {
        this.id = id;
        return this;
    }

    public ConversationTestBuilder type(ConversationType type) {
        this.type = type;
        return this;
    }

    public ConversationTestBuilder createdAt(Instant createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ConversationTestBuilder lastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
        return this;
    }
    public ConversationTestBuilder name(String name) {
        this.name = name;
        return this;
    }

    public ConversationTestBuilder participants(User first, User second) {
        this.firstParticipant = first;
        this.secondParticipant = second;
        return this;
    }

    public Conversation build() {
        Conversation conversation = buildWithoutParticipants();

        ConversationParticipant first = ConversationParticipantTestBuilder.firstConversationParticipant()
                .user(firstParticipant)
                .userNameAtJoin(firstParticipant == null ? null : firstParticipant.getFullName())
                .conversation(conversation)
                .joinedAt(createdAt)
                .build();

        ConversationParticipant second = ConversationParticipantTestBuilder.secondConversationParticipant()
                .user(secondParticipant)
                .userNameAtJoin(secondParticipant == null ? null : secondParticipant.getFullName())
                .conversation(conversation)
                .joinedAt(createdAt)
                .build();

        conversation.setParticipants(new HashSet<>(Set.of(first, second)));
        return conversation;
    }

    /** Minimal parent for association fixtures; does not construct participants recursively. */
    public Conversation buildWithoutParticipants() {
        return Conversation.builder()
                .id(id)
                .name(name)
                .type(type)
                .createdAt(createdAt)
                .lastActiveAt(lastActiveAt)
                .build();
    }
}
