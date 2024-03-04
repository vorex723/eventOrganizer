package com.mazurek.eventOrganizer.thread.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreadCreateDto {
    @NotBlank(message = "Thread name can not be shorter than 10 characters and longer than 140")
    @Size(min = 10, max = 140, message = "Thread name name can not be shorter than 10 characters and longer than 140")
    private String name;
    @NotBlank(message = "First post in thread can not be shorter than 20 characters and longer than 1000")
    @Size(min = 20, max = 1000, message = "First post in thread can not be shorter than 20 characters and longer than 1000")
    private String content;
}
