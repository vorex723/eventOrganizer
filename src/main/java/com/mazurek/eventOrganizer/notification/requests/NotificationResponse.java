package com.mazurek.eventOrganizer.notification.requests;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {
    private int status;
    private String message;
}
