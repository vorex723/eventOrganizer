package com.mazurek.eventOrganizer.conversation;


import com.mazurek.eventOrganizer.conversation.dto.ConversationDto;
import com.mazurek.eventOrganizer.conversation.mapper.ConversationMapper;
import com.mazurek.eventOrganizer.exception.converastion.ConversationNotFoundException;
import com.mazurek.eventOrganizer.exception.converastion.MessagingYourselfException;
import com.mazurek.eventOrganizer.exception.user.UserNotFoundException;
import com.mazurek.eventOrganizer.jwt.JwtUtil;
import com.mazurek.eventOrganizer.notification.NotificationService;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import com.mazurek.eventOrganizer.utils.EncryptionUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationService {
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final EncryptionUtils encryptionUtils;
    private final JwtUtil jwtUtil;
    private final NotificationService notificationService;
    private final ConversationMapper conversationMapper;

    public ConversationDto sendMessage(SendMessageDto messageDto, String jwtToken){

        User sender = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();
        User recipient = userRepository.findById(messageDto.getRecipientId()).orElseThrow(UserNotFoundException::new);
        if (sender.equals(recipient))
            throw new MessagingYourselfException("You can not send messages to yourself.");
        Conversation conversation;
        try {
           conversation = sender.getConversationByUser(recipient);
        } catch (ConversationNotFoundException exception){
            conversation = conversationRepository.save(new Conversation(sender, recipient));
            sender.addConversation(conversation);
            recipient.addConversation(conversation);
            userRepository.save(sender);
            userRepository.save(recipient);
        }

        conversation.addMessage(messageRepository.save(new Message(sender, encryptionUtils.encryptMessage(messageDto.getMessage()))));

        Conversation savedConversation = conversationRepository.save(conversation);

        notificationService.sendNewPrivateMessageNotification(recipient, sender.getFullName());

        encryptionUtils.decryptMessagesInConversation(savedConversation);
        return conversationMapper.mapConversationToConversationDto(savedConversation);
    }

    public ConversationDto getConversationById(UUID conversationId, String jwtToken) {
        User user = userRepository.findByEmail(jwtUtil.extractUsername(jwtToken)).get();
        Conversation conversation = user.getConversationById(conversationId);
        encryptionUtils.decryptMessagesInConversation(conversation);
        return conversationMapper.mapConversationToConversationDto(conversation);
    }
}
