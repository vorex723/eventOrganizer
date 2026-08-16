package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair.DirectPairIds;
import com.mazurek.eventOrganizer.conversation.dto.*;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.conversation.ConversationNotFoundException;
import com.mazurek.eventOrganizer.exception.conversation.ConversationParticipantNotFound;
import com.mazurek.eventOrganizer.exception.conversation.MessagingYourselfException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.service.NotificationCommandService;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ConversationServiceImpl implements ConversationService {
    private final AuthenticationService authenticationService;
    private final NotificationCommandService notificationCommandService;
    private final ConversationRepository conversationRepository;
    private final ConversationCreationService conversationCreationService;
    private final DirectConversationPairRepository directConversationPairRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final EncryptionUtils encryptionUtils;
    private final PaginationProperties paginationProperties;
    private final ConversationParticipantRepository participantRepository;
    private final Clock clock;


    @Override
    @Transactional
    public DirectMessageResponseDto sendDirectMessage(SendDirectMessageDto sendDirectMessageDto) {

        User sender = authenticationService.getCurrentUser();

        if (sender.getId().equals(sendDirectMessageDto.recipientId()))
            throw new MessagingYourselfException();

        User recipient = userRepository.findById(sendDirectMessageDto.recipientId())
                .orElseThrow(UserNotFoundException::new);

        Instant createdAt = Instant.now(clock);
        Conversation conversation;
        boolean conversationCreated;
        DirectPairIds directPairIds = DirectPairIds.of(sender.getId(), recipient.getId());

        Optional<Conversation> existingConversation = directConversationPairRepository
                .findConversationByUsers(directPairIds.firstUserId(), directPairIds.secondUserId());

        if (existingConversation.isPresent()) {
            conversation = existingConversation.get();
            conversationCreated = false;
        } else {
            try {
                conversationCreationService.createDirectConversation(sender, recipient, createdAt);
                conversationCreated = true;
            } catch (DataIntegrityViolationException exception) {
                conversationCreated = false;
            }
            conversation = directConversationPairRepository
                    .findConversationByUsers(directPairIds.firstUserId(), directPairIds.secondUserId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Direct conversation should exist after creation attempt"
                    ));
        }

        ConversationParticipant senderParticipant = conversation.getParticipants()
                .stream()
                .filter(participant -> participant.getUser().getId().equals(sender.getId()))
                .findFirst()
                .orElseThrow();

        MessageDto messageDto = appendMessage(
                conversation,
                senderParticipant,
                sender,
                sendDirectMessageDto.content(),
                createdAt
        );

        notificationCommandService.notifyPrivateMessage(
                conversation.getId(),
                recipient.getId(),
                sender.getFullName()
        );

        return new DirectMessageResponseDto(conversation.getId(), conversationCreated, messageDto);
    }

    @Override
    @Transactional
    public MessageDto sendMessageToConversation(
            UUID conversationId,
            SendConversationMessageDto sendConversationMessageDto
    ) {
        User sender = authenticationService.getCurrentUser();

        Conversation conversation = conversationRepository
                .findByIdAndParticipantId(conversationId, sender.getId())
                .orElseThrow(ConversationNotFoundException::new);

        ConversationParticipant senderParticipant = participantRepository
                .findByConversationIdAndUserId(conversationId, sender.getId())
                .orElseThrow(ConversationParticipantNotFound::new);

        return appendMessage(
                conversation,
                senderParticipant,
                sender,
                sendConversationMessageDto.content(),
                Instant.now(clock)
        );
    }

    @Override
    @Transactional
    public MessagePageDto getMessagesInConversation(UUID conversationId, int pageNumber) {
        if (pageNumber < 0)
            throw new InvalidPageNumberException();

        UUID currentUserId = authenticationService.getCurrentUserId();

        if (!conversationRepository.existsByIdAndParticipant(conversationId, currentUserId))
            throw new ConversationNotFoundException();

        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                paginationProperties.getDefaultPageSize(),
                Sort.by(Sort.Direction.DESC, "sentDate", "id"));
        Page<Message> messagePage = messageRepository.findByConversationId(conversationId, pageRequest);

        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseThrow(ConversationParticipantNotFound::new);

        if (pageNumber == 0) {
            Instant now = clock.instant();
            participant.setLastReadAt(now);

            messagePage.getContent().stream()
                    .findFirst()
                    .map(Message::getId)
                    .ifPresent(participant::setLastReadMessageId);
        }

        MessagePageDto messagePageDto = new MessagePageDto(messagePage);
        messagePageDto.messages().forEach(messageDto ->
                messageDto.setContent(encryptionUtils.decryptMessage(messageDto.getContent())));

        return messagePageDto;
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationOverviewPageDto getConversations(int pageNumber) {
        if (pageNumber < 0)
            throw new InvalidPageNumberException();

        PageRequest pageRequest = PageRequest.of(
                pageNumber,
                paginationProperties.getDefaultPageSize(),
                Sort.by(Sort.Direction.DESC, "lastActiveAt", "id")
        );

        UUID userId = authenticationService.getCurrentUserId();

        Page<Conversation> conversationPage = conversationRepository.findByParticipantId(userId, pageRequest);

        return new ConversationOverviewPageDto(conversationPage, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationDetailsDto getConversation(UUID conversationId) {
        UUID userId = authenticationService.getCurrentUserId();
        Conversation conversation = conversationRepository.findByIdAndParticipantId(conversationId, userId).orElseThrow(ConversationNotFoundException::new);
        if (conversation.getType().equals(ConversationType.DIRECT)) {
            String directConversationName = conversation
                    .getParticipants()
                    .stream()
                    .filter(participant -> !participant.getUser().getId().equals(userId))
                    .findFirst().orElseThrow(IllegalStateException::new)
                    .getUser()
                    .getFullName();
            return new ConversationDetailsDto(conversation, directConversationName);
        }
        return new ConversationDetailsDto(conversation);
    }

    private MessageDto appendMessage(
            Conversation conversation,
            ConversationParticipant senderParticipant,
            User sender,
            String content,
            Instant sentDate
    ) {
        conversation.setLastActiveAt(sentDate);

        Message message = createMessage(content, conversation, sender, sentDate);

        senderParticipant.setLastReadAt(sentDate);
        senderParticipant.setLastReadMessageId(message.getId());

        MessageDto messageDto = new MessageDto(message);
        messageDto.setContent(encryptionUtils.decryptMessage(messageDto.getContent()));

        return messageDto;
    }

    private Message createMessage(String content, Conversation conversation, User sender, Instant sentDate) {
        return messageRepository.save(
                Message.builder()
                        .conversation(conversation)
                        .sentDate(sentDate)
                        .sender(sender)
                        .content(encryptionUtils.encryptMessage(content))
                        .build()
        );
    }

}
