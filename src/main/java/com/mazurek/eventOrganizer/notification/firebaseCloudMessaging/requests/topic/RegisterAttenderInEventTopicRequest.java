package com.mazurek.eventOrganizer.notification.firebaseCloudMessaging.requests.topic;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterAttenderInEventTopicRequest {
    private String userFcmToken;
    private String eventFcmTopicId;
    private String id;
}
