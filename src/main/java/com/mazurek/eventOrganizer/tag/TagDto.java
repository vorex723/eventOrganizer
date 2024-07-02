package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.event.dto.EventWithoutUsersDto;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagDto {
    private UUID id;
    private String name;
    @Builder.Default
    private Set<EventWithoutUsersDto> events = new HashSet<>();
}
