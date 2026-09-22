package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.conversation.dto.MessageDto;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.notification.service.NotificationCommandService;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
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
    private final MessageRepository messageRepository;
    private final EncryptionUtils encryptionUtils;
    private final NotificationCommandService notificationCommandService;


    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createDirectConversation(User sender, User recipient, Instant createdAt) {
        DirectConversationSetup setup = setupDirectConversation(sender, recipient, createdAt);

        directConversationPairRepository.saveAndFlush(
                DirectConversationPair.of(setup.conversation(), sender.getId(), recipient.getId()));

        return setup.conversation().getId();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InitialDirectMessage createDirectConversationWithInitialMessage(
            User sender,
            User recipient,
            String content,
            Instant createdAt
    ) {
        DirectConversationSetup setup = setupDirectConversation(sender, recipient, createdAt);

        directConversationPairRepository.saveAndFlush(
                DirectConversationPair.of(setup.conversation(), sender.getId(), recipient.getId()));

        Message message = messageRepository.save(Message.builder()
                .conversation(setup.conversation())
                .sentDate(createdAt)
                .sender(sender)
                .content(encryptionUtils.encryptMessage(content))
                .build());

        setup.senderParticipant().setLastReadAt(createdAt);
        setup.senderParticipant().setLastReadMessageId(message.getId());

        notificationCommandService.notifyPrivateMessage(
                setup.conversation().getId(),
                recipient.getId(),
                sender.getFullName());

        return new InitialDirectMessage(
                setup.conversation().getId(),
                new MessageDto(message.getId(), sender.getId(), createdAt, content)
        );
    }

    private DirectConversationSetup setupDirectConversation(User sender, User recipient, Instant createdAt) {
        Conversation conversation = createConversation(createdAt);
        ConversationParticipant senderParticipant = createParticipant(sender, conversation, createdAt);
        ConversationParticipant recipientParticipant = createParticipant(recipient, conversation, createdAt);

        conversation.addParticipant(senderParticipant);
        conversation.addParticipant(recipientParticipant);

        return new DirectConversationSetup(conversation, senderParticipant);
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

    private record DirectConversationSetup(
            Conversation conversation,
            ConversationParticipant senderParticipant
    ) {
    }
}
