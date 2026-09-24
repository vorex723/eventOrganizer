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
import com.mazurek.eventOrganizer.exception.conversation.MessagingYourselfException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.notification.service.NotificationCommandService;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
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
        } else if (directConversationPairRepository.existsByFirstUserIdAndSecondUserId(
                directPairIds.firstUserId(), directPairIds.secondUserId())) {
            throw new ConversationNotFoundException();
        } else {
            try {
                ConversationCreationService.InitialDirectMessage initialMessage = conversationCreationService
                        .createDirectConversationWithInitialMessage(
                                sender,
                                recipient,
                                sendDirectMessageDto.content(),
                                createdAt
                        );

                return new DirectMessageResponseDto(
                        initialMessage.conversationId(),
                        true,
                        initialMessage.message()
                );
            } catch (DataIntegrityViolationException exception) {
                if (!isDirectConversationPairConflict(exception)) {
                    throw exception;
                }
                conversationCreated = false;
            }
            conversation = directConversationPairRepository
                    .findConversationByUsers(directPairIds.firstUserId(), directPairIds.secondUserId())
                    .orElseThrow(ConversationNotFoundException::new);
        }

        MessageDto messageDto = appendMessage(
                conversation,
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

        ensureDirectConversationHasActiveParticipants(conversation);

        MessageDto messageDto = appendMessage(
                conversation,
                sender,
                sendConversationMessageDto.content(),
                Instant.now(clock)
        );

        if (conversation.getType() == ConversationType.DIRECT) {
            notificationCommandService.notifyPrivateMessage(
                    conversationId,
                    getDirectConversationRecipientId(conversation, sender.getId()),
                    sender.getFullName()
            );
        }

        return messageDto;
    }

    @Override
    @Transactional(readOnly = true)
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

        List<MessageDto> messages = messagePage.getContent().stream()
                .map(message -> {
                    MessageDto messageDto = new MessageDto(message);
                    decryptForResponse(message, messageDto);
                    return messageDto;
                })
                .toList();

        return new MessagePageDto(messagePage).withMessages(messages);
    }

    @Override
    @Transactional
    public void markConversationRead(UUID conversationId, MarkConversationReadDto markConversationReadDto) {
        UUID currentUserId = authenticationService.getCurrentUserId();

        if (!conversationRepository.existsByIdAndParticipant(conversationId, currentUserId)) {
            throw new ConversationNotFoundException();
        }
        if (!messageRepository.existsByIdAndConversationId(markConversationReadDto.lastReadMessageId(), conversationId)) {
            throw new ConversationNotFoundException();
        }

        participantRepository.advanceLastReadMessage(
                conversationId,
                currentUserId,
                markConversationReadDto.lastReadMessageId(),
                clock.instant()
        );
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
            ConversationParticipant directParticipant = conversation
                    .getParticipants()
                    .stream()
                    .filter(participant -> participant.getUser() == null || !participant.getUser().getId().equals(userId))
                    .findFirst().orElseThrow(IllegalStateException::new);
            String directConversationName = directParticipant.getUserNameAtJoin() != null
                    ? directParticipant.getUserNameAtJoin()
                    : directParticipant.getUser() == null ? "Deleted user" : directParticipant.getUser().getFullName();
            return new ConversationDetailsDto(conversation, directConversationName);
        }
        return new ConversationDetailsDto(conversation);
    }

    private MessageDto appendMessage(
            Conversation conversation,
            User sender,
            String content,
            Instant sentDate
    ) {
        Message message = createMessage(content, conversation, sender, sentDate);
        participantRepository.advanceLastReadMessage(
                conversation.getId(),
                sender.getId(),
                message.getId(),
                sentDate
        );
        conversationRepository.advanceLastActivity(conversation.getId(), sentDate);

        MessageDto messageDto = new MessageDto(message);
        decryptForResponse(message, messageDto);

        return messageDto;
    }

    private Message createMessage(String content, Conversation conversation, User sender, Instant sentDate) {
        EncryptionUtils.EncryptedConversationContent encryptedContent = encryptionUtils.encryptConversationMessage(content);
        return messageRepository.save(
                Message.builder()
                        .conversation(conversation)
                        .sentDate(sentDate)
                        .sender(sender)
                        .senderNameAtCreation(sender.getFullName())
                        .encryptionKeyId(encryptedContent.keyId())
                        .content(encryptedContent.ciphertext())
                        .build()
        );
    }

    private void decryptForResponse(Message message, MessageDto messageDto) {
        encryptionUtils.decryptConversationMessage(message.getContent(), message.getEncryptionKeyId())
                .ifPresentOrElse(
                        messageDto::setContent,
                        () -> {
                            messageDto.setContent(null);
                            messageDto.setContentUnavailable(true);
                            log.warn("Conversation message {} cannot be decrypted with key {}", message.getId(), message.getEncryptionKeyId());
                        }
                );
    }

    private UUID getDirectConversationRecipientId(Conversation conversation, UUID senderId) {
        return conversation.getParticipants().stream()
                .map(ConversationParticipant::getUser)
                .filter(java.util.Objects::nonNull)
                .map(User::getId)
                .filter(participantId -> !participantId.equals(senderId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Direct conversation must contain a participant other than the sender"
                ));
    }

    private void ensureDirectConversationHasActiveParticipants(Conversation conversation) {
        if (conversation.getType() != ConversationType.DIRECT) {
            return;
        }

        long activeParticipantCount = conversation.getParticipants().stream()
                .filter(participant -> participant.getLeftAt() == null && participant.getUser() != null)
                .count();
        if (activeParticipantCount != 2) {
            throw new ConversationNotFoundException();
        }
    }

    private boolean isDirectConversationPairConflict(DataIntegrityViolationException exception) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof org.hibernate.exception.ConstraintViolationException constraintViolation
                    && "uq_direct_conversation_pair_users".equals(constraintViolation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }

        return false;
    }

}
