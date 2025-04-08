package com.mazurek.eventOrganizer.tag;

import com.mazurek.eventOrganizer.event.dto.EventOverviewDto;
import lombok.*;

import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagDto {
    private UUID id;
    private String name;
    @Builder.Default
    private List<EventOverviewDto> events = new ArrayList<>();

    public TagDto(Tag tag) {
        this.id = tag.getId();
        this.name = tag.getName();
        this.events = tag.getEvents().stream().map(EventOverviewDto::new).toList();
    }
}
