package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.conversation.Conversation;
import com.mazurek.eventOrganizer.conversation.Message;
import com.mazurek.eventOrganizer.user.User;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.mazurek.eventOrganizer.testData.TestConstants.*;

/**
 * Builder for creating Message test objects with sensible defaults.
 */
public class MessageTestBuilder {

    private UUID id = MessageConstants.FIRST_MESSAGE_ID;
    private Conversation conversation = ConversationTestBuilder.firstConversation().build();
    private User sender = UserTestBuilder.firstUser().build();
    private String message = MessageConstants.FIRST_MESSAGE_CONTENT;
    private LocalDateTime sentDate = TimeConstants.LOCAL_DATE_TIME_NOW;

    public static MessageTestBuilder firstMessage() {
        return new MessageTestBuilder()
                .id(MessageConstants.FIRST_MESSAGE_ID)
                .sender(UserTestBuilder.firstUser().build())
                .message(MessageConstants.FIRST_MESSAGE_CONTENT);
    }

    public static MessageTestBuilder secondMessage() {
        return new MessageTestBuilder()
                .id(MessageConstants.SECOND_MESSAGE_ID)
                .sender(UserTestBuilder.secondUser().build())
                .message(MessageConstants.SECOND_MESSAGE_CONTENT);
    }

    public static MessageTestBuilder thirdMessage() {
        return new MessageTestBuilder()
                .id(MessageConstants.THIRD_MESSAGE_ID)
                .sender(UserTestBuilder.firstUser().build())
                .message(MessageConstants.THIRD_MESSAGE_CONTENT);
    }

    public MessageTestBuilder id(UUID id) {
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

    public MessageTestBuilder message(String message) {
        this.message = message;
        return this;
    }

    public MessageTestBuilder sentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
        return this;
    }

    public Message build() {
        return Message.builder()
                .id(id)
                .conversation(conversation)
                .sender(sender)
                .message(message)
                .sentDate(sentDate)
                .build();
    }
}
