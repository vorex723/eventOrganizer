package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.user.User;

import java.time.Instant;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

/**
 * Builder for creating Message test objects with sensible defaults.
 */
public class MessageTestBuilder {

    private Long id = MessageConstants.FIRST_MESSAGE_ID;
    private Conversation conversation = ConversationTestBuilder.firstConversation().build();
    private User sender = UserTestBuilder.firstUser().build();
    private String content = MessageConstants.FIRST_MESSAGE_CONTENT;
    private Instant sentDate = TimeConstants.NOW;

    public static MessageTestBuilder firstMessage() {
        return new MessageTestBuilder()
                .id(MessageConstants.FIRST_MESSAGE_ID)
                .sender(UserTestBuilder.firstUser().build())
                .content(MessageConstants.FIRST_MESSAGE_CONTENT);
    }

    public static MessageTestBuilder inverseMessage() {
        return new MessageTestBuilder()
                .id(MessageConstants.SECOND_MESSAGE_ID)
                .sender(UserTestBuilder.secondUser().build())
                .content(MessageConstants.SECOND_MESSAGE_CONTENT);
    }

    public static MessageTestBuilder thirdMessage() {
        return new MessageTestBuilder()
                .id(MessageConstants.THIRD_MESSAGE_ID)
                .sender(UserTestBuilder.firstUser().build())
                .content(MessageConstants.THIRD_MESSAGE_CONTENT);
    }

    public MessageTestBuilder id(Long id) {
        this.id = id;
        return this;
    }

    public MessageTestBuilder conversation(Conversation conversation) {
        this.conversation = conversation;
        return this;
    }

    public MessageTestBuilder sender(User sender) {
        this.sender = sender;
        return this;
    }

    public MessageTestBuilder content(String content) {
        this.content = content;
        return this;
    }

    public MessageTestBuilder sentDate(Instant sentDate) {
        this.sentDate = sentDate;
        return this;
    }

    public Message build() {
        return Message.builder()
                .id(id)
                .conversation(conversation)
                .sender(sender)
                .content(content)
                .sentDate(sentDate)
                .build();
    }
}
