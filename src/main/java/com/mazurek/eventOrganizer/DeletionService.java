package com.mazurek.eventOrganizer;

import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.conversation.ConversationRepository;
import com.mazurek.eventOrganizer.conversation.direct.DirectConversationPairRepository;
import com.mazurek.eventOrganizer.conversation.message.MessageRepository;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.jwt.RefreshTokenRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationRepository;
import com.mazurek.eventOrganizer.notification.repository.NotificationPreferenceRepository;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.threadReply.ThreadReplyRepository;
import com.mazurek.eventOrganizer.thread.ThreadRepository;
import com.mazurek.eventOrganizer.user.RoleRepository;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletionService {
    private final ActivationTokenRepository activationTokenRepository;
    private final RoleRepository roleRepository;
    private final CityRepository cityRepository;
    private final TagRepository tagRepository;
    private final FileRepository fileRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final DirectConversationPairRepository directConversationPairRepository;
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ThreadRepository threadRepository;
    private final ThreadReplyRepository threadReplyRepository;

@Transactional
public void deleteAllSafe() {

    eventRepository.findAll().forEach(event -> {
        event.getAttendingUsers().clear();
        event.getTags().clear();
    });

    userRepository.findAll().forEach(user -> {
        user.getRoles().clear();
    });

    conversationRepository.findAll().forEach(conversation -> conversation.getParticipants().clear());

    eventRepository.flush();
    userRepository.flush();
    conversationRepository.flush();

    threadReplyRepository.deleteAll();
    threadRepository.deleteAll();
    fileRepository.deleteAll();
    messageRepository.deleteAll();
    directConversationPairRepository.deleteAll();
    notificationRepository.deleteAll();
    notificationPreferenceRepository.deleteAll();
    activationTokenRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    threadReplyRepository.flush();
    threadRepository.flush();
    fileRepository.flush();
    messageRepository.flush();
    directConversationPairRepository.flush();
    notificationRepository.flush();
    notificationPreferenceRepository.flush();
    activationTokenRepository.flush();
    refreshTokenRepository.flush();

    eventRepository.deleteAll();
    conversationRepository.deleteAll();
    userRepository.deleteAll();
    tagRepository.deleteAll();
    cityRepository.deleteAll();
    roleRepository.deleteAll();
    eventRepository.flush();
    conversationRepository.flush();
    userRepository.flush();
    tagRepository.flush();
    cityRepository.flush();
    roleRepository.flush();
}

}
