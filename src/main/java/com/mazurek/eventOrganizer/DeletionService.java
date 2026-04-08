package com.mazurek.eventOrganizer;

import com.mazurek.eventOrganizer.auth.ActivationTokenRepository;
import com.mazurek.eventOrganizer.city.CityRepository;
import com.mazurek.eventOrganizer.conversation.ConversationRepository;
import com.mazurek.eventOrganizer.conversation.MessageRepository;
import com.mazurek.eventOrganizer.event.EventRepository;
import com.mazurek.eventOrganizer.file.FileRepository;
import com.mazurek.eventOrganizer.jwt.RefreshTokenRepository;
import com.mazurek.eventOrganizer.notification.NotificationRepository;
import com.mazurek.eventOrganizer.tag.TagRepository;
import com.mazurek.eventOrganizer.thread.ThreadReplyRepository;
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
    private final MessageRepository messageRepository;
    private final NotificationRepository notificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final ThreadRepository threadRepository;
    private final ThreadReplyRepository threadReplyRepository;
/*
    @Transactional
    public void deleteAll(){
        tagRepository.findAll().forEach(tag -> tag.setEvents(null));
        threadReplyRepository.findAll().forEach(threadReply -> {
            threadReply.setThread(null);
            threadReply.setReplier(null);
        });
        threadRepository.findAll().forEach(thread -> {
            thread.setOwner(null);
            thread.setEvent(null);
            thread.setReplies(null);
        });
        fileRepository.findAll().forEach(file -> {
            file.setEvent(null);
            file.setOwner(null);
        });
        eventRepository.findAll().forEach(event -> {
            event.setCity(null);
            event.setOwner(null);
            //event.setFiles(null);
            //event.setThreads(null);
            event.setTags(null);
            event.setAttendingUsers(null);
        });
        messageRepository.findAll().forEach(message -> {
            message.setConversation(null);
            message.setSender(null);
        });
        conversationRepository.findAll().forEach(conversation -> {
            conversation.setParticipants(null);
            conversation.setMessages(null);
        });
        notificationRepository.findAll().forEach(notification -> notification.setReceiver(null));
        userRepository.findAll().forEach(user -> {
            user.setHomeCity(null);
            user.setAttendingEvents(null);
            user.setUserEvents(null);;
            user.setConversations(null);
            user.setNotifications(null);
            user.setFiles(null);
            user.setThreadReplies(null);
            user.setThreads(null);
            user.setRoles(null);
        });
        cityRepository.findAll().forEach(city -> {
            city.setEvents(null);
            city.setResidents(null);
        });


        activationTokenRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        notificationRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        tagRepository.deleteAll();
        threadReplyRepository.deleteAll();
        threadRepository.deleteAll();
        fileRepository.deleteAll();
        eventRepository.deleteAll();
        userRepository.deleteAll();
        cityRepository.deleteAll();
        roleRepository.deleteAll();
    }*/
@Transactional
public void deleteAllSafe() {
    // Rozpinamy many-to-many po stronie wlasciciela relacji.
    eventRepository.findAll().forEach(event -> {
        event.getAttendingUsers().clear();
        event.getTags().clear();
    });

    userRepository.findAll().forEach(user -> {
        user.getRoles().clear();
        user.getConversations().clear();
    });

    conversationRepository.findAll().forEach(conversation -> conversation.getParticipants().clear());

    // Wymuszamy zapis zmian w join table przed usuwaniem encji.
    eventRepository.flush();
    userRepository.flush();
    conversationRepository.flush();

    // Usuwamy encje zalezne (dzieci) najpierw.
    threadReplyRepository.deleteAll();
    threadRepository.deleteAll();
    fileRepository.deleteAll();
    messageRepository.deleteAll();
    notificationRepository.deleteAll();
    activationTokenRepository.deleteAll();
    refreshTokenRepository.deleteAll();
    threadReplyRepository.flush();
    threadRepository.flush();
    fileRepository.flush();
    messageRepository.flush();
    notificationRepository.flush();
    activationTokenRepository.flush();
    refreshTokenRepository.flush();

    // Usuwamy encje nadrzedne.
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
