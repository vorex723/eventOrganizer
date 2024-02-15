package com.mazurek.eventOrganizer.file;

import com.mazurek.eventOrganizer.user.dto.UserWithoutEventsDto;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FileDto {
    private UUID id;
    private String name;
    private UserWithoutEventsDto owner;

}
