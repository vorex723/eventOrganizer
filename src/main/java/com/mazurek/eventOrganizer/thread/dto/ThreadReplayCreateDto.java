package com.mazurek.eventOrganizer.thread.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadReplayCreateDto {
    @NotBlank(message = "Reply in thread can not be shorter than 20 characters and longer than 1000")
    @Size(min = 20, max = 1000, message = "Reply in thread can not be shorter than 20 characters and longer than 1000")
    private String replyContent;
}
