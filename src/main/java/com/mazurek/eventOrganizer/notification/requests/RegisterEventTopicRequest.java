package com.mazurek.eventOrganizer.notification.requests;

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
}
