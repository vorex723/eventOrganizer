package com.mazurek.eventOrganizer.notification.requests;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SingleUserNotificationRequest {

    private String receiverFcmToken;
    private String title;
    private String body;

}
