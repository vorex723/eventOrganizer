package com.mazurek.eventOrganizer.notification.requests;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterAttenderInEventTopicRequest {
    private String userFcmToken;
    private String eventFcmTopicId;
    private String notificationType;
    private String id;
}
