package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.event.dto.EventWithoutUsersDto;
import lombok.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagDto {
    private Long id;
    private String name;
    private Set<EventWithoutUsersDto> events = new HashSet<>();
}
