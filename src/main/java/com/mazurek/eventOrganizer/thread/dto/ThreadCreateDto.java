package com.mazurek.eventOrganizer.thread.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadCreateDto {
    private String name;
    private String content;
}
