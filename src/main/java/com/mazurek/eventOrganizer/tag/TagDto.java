package com.mazurek.eventOrganizer.tag;

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
    private long eventCount;

    public TagDto(Tag tag) {
        this.id = tag.getId();
        this.name = tag.getName();
        this.eventCount = 0;
    }
}
