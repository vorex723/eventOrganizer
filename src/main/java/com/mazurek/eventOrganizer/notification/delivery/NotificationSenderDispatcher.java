package com.mazurek.eventOrganizer.notification.delivery;

import com.mazurek.eventOrganizer.notification.domain.NotificationChannel;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class NotificationSenderDispatcher {
    private final Map<NotificationChannel, NotificationSender> senders;

    public NotificationSenderDispatcher(List<NotificationSender> senders) {
        this.senders = senders.stream().collect(Collectors.toMap(NotificationSender::supportedChannel, Function.identity()));
    }

    public NotificationSendResult send(NotificationChannel channel, NotificationSendRequest request) {
        NotificationSender sender = senders.get(channel);
        if (sender == null) {
            return NotificationSendResult.failed("No sender registered for channel " + channel);
        }
        return sender.send(request);
    }
}
