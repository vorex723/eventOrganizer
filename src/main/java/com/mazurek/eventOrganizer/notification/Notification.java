package com.mazurek.eventOrganizer.notification;

import com.mazurek.eventOrganizer.user.User;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    private User receiver;
    @Builder.Default
    private Boolean opened = false;
    @Builder.Default
    private String notificationTitle = "";
    @Builder.Default
    private String notificationBody = "";
    private NotificationType notificationType;
    private UUID resourceId;
}
