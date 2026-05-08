package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationCreationServiceImpl implements ConversationCreationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final DirectConversationPairRepository directConversationPairRepository;


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createDirectConversation(User sender, User recipient, Instant createdAt) {
        Conversation conversation = setupDirectConversation(sender, recipient, createdAt);

        directConversationPairRepository.saveAndFlush(
                DirectConversationPair.of(conversation, sender.getId(), recipient.getId()));

        return conversation.getId();
    }

    private Conversation setupDirectConversation(User sender, User recipient, Instant createdAt) {
        Conversation conversation = createConversation(createdAt);
        ConversationParticipant senderParticipant = createParticipant(sender, conversation, createdAt);
        ConversationParticipant recipientParticipant = createParticipant(recipient, conversation, createdAt);

        conversation.addParticipant(senderParticipant);
        conversation.addParticipant(recipientParticipant);

        return conversation;
    }

    private ConversationParticipant createParticipant(User user, Conversation conversation, Instant createdAt) {
        return participantRepository.save(ConversationParticipant.builder()
                .user(user)
                .conversation(conversation)
                .joinedAt(createdAt)
                .build());
    }

    private Conversation createConversation(Instant createdAt) {
        return conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .createdAt(createdAt)
                .lastActiveAt(createdAt)
                .build());
    }
}
