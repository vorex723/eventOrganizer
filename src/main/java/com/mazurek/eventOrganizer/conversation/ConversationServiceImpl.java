package com.mazurek.eventOrganizer.conversation;

import com.mazurek.eventOrganizer.auth.AuthenticationService;
import com.mazurek.eventOrganizer.config.properties.PaginationProperties;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPair.DirectPairIds;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipant;
import com.mazurek.eventOrganizer.conversation.participant.ConversationParticipantRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.conversation.dto.DirectMessageResponseDto;
import com.mazurek.eventOrganizer.conversation.dto.MessageDto;
import com.mazurek.eventOrganizer.conversation.dto.MessagePageDto;
import com.mazurek.eventOrganizer.conversation.dto.SendDirectMessageDto;
import com.mazurek.eventOrganizer.conversation.message.Message;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.exception.common.InvalidPageNumberException;
import com.mazurek.eventOrganizer.exception.conversation.ConversationNotFoundException;
import com.mazurek.eventOrganizer.exception.conversation.ConversationParticipantNotFound;
import com.mazurek.eventOrganizer.exception.conversation.MessagingYourselfException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
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
    private final ConversationRepository conversationRepository;
    private final ConversationCreationService  conversationCreationService;
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

        if (sender.getId().equals(sendDirectMessageDto.getRecipientId()))
            throw new MessagingYourselfException();

        User recipient = userRepository.findById(sendDirectMessageDto.getRecipientId())
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
            try{
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

        conversation.setLastActiveAt(createdAt);

        Message message = createMessage(sendDirectMessageDto, conversation, sender, createdAt);

        senderParticipant.setLastReadAt(createdAt);
        senderParticipant.setLastReadMessageId(message.getId());

        MessageDto messageDto = new MessageDto(message);
        messageDto.setContent(encryptionUtils.decryptMessage(messageDto.getContent()));

        return new DirectMessageResponseDto(conversation.getId(), conversationCreated, messageDto);
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

        if (pageNumber == 0){
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


    private Message createMessage(SendDirectMessageDto sendDirectMessageDto, Conversation conversation, User sender, Instant sentDate) {
        return messageRepository.save(
                Message.builder()
                        .conversation(conversation)
                        .sentDate(sentDate)
                        .sender(sender)
                        .content(encryptionUtils.encryptMessage(sendDirectMessageDto.getContent()))
                        .build()
        );
    }

}
