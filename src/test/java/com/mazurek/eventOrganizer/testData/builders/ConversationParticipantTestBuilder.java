package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.user.User;

import java.time.Instant;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

public class ConversationParticipantTestBuilder {

    private Long id = ConversationParticipantConstants.FIRST_CONVERSATION_PARTICIPANT_ID;
    private User user = UserTestBuilder.firstUser().build();
    private Conversation conversation;
    private Instant joinedAt = TimeConstants.NOW;
    private Instant leftAt;
    private Instant lastReadAt = TimeConstants.NOW;
    private Long lastReadMessageId = MessageConstants.FIRST_MESSAGE_ID;

    public static ConversationParticipantTestBuilder firstConversationParticipant() {
        return new ConversationParticipantTestBuilder()
                .id(ConversationParticipantConstants.FIRST_CONVERSATION_PARTICIPANT_ID)
                .user(UserTestBuilder.firstUser().build())
                .joinedAt(TimeConstants.NOW)
                .lastReadAt(TimeConstants.NOW)
                .lastReadMessageId(MessageConstants.FIRST_MESSAGE_ID);
    }

    public static ConversationParticipantTestBuilder secondConversationParticipant() {
        return new ConversationParticipantTestBuilder()
                .id(ConversationParticipantConstants.SECOND_CONVERSATION_PARTICIPANT_ID)
                .user(UserTestBuilder.secondUser().build())
                .joinedAt(TimeConstants.NOW)
                .lastReadAt(null)
                .lastReadMessageId(null);
    }

    public ConversationParticipantTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public ConversationParticipantTestBuilder user(User user) {
        this.user = user;
        return this;
    }

    public ConversationParticipantTestBuilder conversation(Conversation conversation) {
        this.conversation = conversation;
        return this;
    }

    public ConversationParticipantTestBuilder joinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
        return this;
    }

    public ConversationParticipantTestBuilder leftAt(Instant leftAt) {
        this.leftAt = leftAt;
        return this;
    }

    public ConversationParticipantTestBuilder lastReadAt(Instant lastReadAt) {
        this.lastReadAt = lastReadAt;
        return this;
    }

    public ConversationParticipantTestBuilder lastReadMessageId(Long lastReadMessageId) {
        this.lastReadMessageId = lastReadMessageId;
        return this;
    }

    public ConversationParticipant build() {
        return ConversationParticipant.builder()
                .id(id)
                .user(user)
                .conversation(conversation)
                .joinedAt(joinedAt)
                .leftAt(leftAt)
                .lastReadAt(lastReadAt)
                .lastReadMessageId(lastReadMessageId)
                .build();
    }
}
