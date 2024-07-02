package com.mazurek.eventOrganizer.notification.requests;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopicNotificationRequest {
    private String fcmTopicId;
    private String title;
    private String body;
    private String notificationType;
    private String id;


}
