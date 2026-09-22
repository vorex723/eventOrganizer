package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.exception.auth.UserNotAuthenticatedException;
import com.mazurek.eventOrganizer.notification.domain.Notification;
import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import com.mazurek.eventOrganizer.user.User;
import com.mazurek.eventOrganizer.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

//@Component
@RequiredArgsConstructor
public class EmailNotificationSender implements NotificationSender {

    private final UserRepository userRepository;
    private final NotificationEmailClient emailClient;

    @Override
    public NotificationChannel supportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public NotificationSendResult send(Notification request) {
        User recipient  = userRepository.findById(request.getRecipientId())
                .orElseThrow(UserNotAuthenticatedException::new);

        return emailClient.send(recipient.getEmail(), request.getTitle(), request.getBody());
    }
}
