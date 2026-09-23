package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.conversation.dto.ConversationParticipantDto;
import com.mazurek.eventOrganizer.conversation.dto.MessageDto;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import org.junit.jupiter.api.Test;

import static com.mazurek.eventOrganizer.testData.builders.MessageTestBuilder.firstMessage;
import static com.mazurek.eventOrganizer.testData.builders.ConversationParticipantTestBuilder.firstConversationParticipant;
import static org.assertj.core.api.Assertions.assertThat;

class ConversationCompatibilityDtoUnitTest {

    @Test
    void mapsLegacyMessageWithMissingSenderFromItsSnapshot() {
        Message message = firstMessage()
                .sender(null)
                .senderNameAtCreation("Deleted user")
                .build();

        MessageDto dto = new MessageDto(message);

        assertThat(dto.getSenderId()).isNull();
        assertThat(dto.getSenderName()).isEqualTo("Deleted user");
    }

    @Test
    void mapsLegacyParticipantWithMissingUserFromItsSnapshot() {
        ConversationParticipant participant = firstConversationParticipant()
                .user(null)
                .userNameAtJoin("Deleted user")
                .build();

        ConversationParticipantDto dto = new ConversationParticipantDto(participant);

        assertThat(dto.userId()).isNull();
        assertThat(dto.fullName()).isEqualTo("Deleted user");
    }
}
