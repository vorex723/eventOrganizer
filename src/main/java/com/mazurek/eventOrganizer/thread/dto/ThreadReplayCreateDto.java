package com.mazurek.eventOrganizer.thread.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadReplayCreateDto {
    private String replyContent;
}
