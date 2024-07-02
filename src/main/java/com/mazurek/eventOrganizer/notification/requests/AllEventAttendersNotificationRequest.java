package com.mazurek.eventOrganizer.notification.requests;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllEventAttendersNotificationRequest {

    @Builder.Default
    private List<String> eventAttendersFcmTokenList = new ArrayList<>();
    private String title;
    private String body;
    private String notificationType;
    private String id;
}
