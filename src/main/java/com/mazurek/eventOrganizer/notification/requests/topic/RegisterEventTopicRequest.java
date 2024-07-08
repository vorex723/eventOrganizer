package com.mazurek.eventOrganizer.notification.requests.topic;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterEventTopicRequest {
    private String eventOwnerFcmToken;
    private String eventFcmTopicId;
    private String title;
    private String body;
    private String notificationType;
    private String id;
}
