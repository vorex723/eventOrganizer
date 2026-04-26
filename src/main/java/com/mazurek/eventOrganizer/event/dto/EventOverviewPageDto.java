package com.mazurek.eventOrganizer.event.dto;

import com.mazurek.eventOrganizer.event.Event;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventOverviewPageDto {
    private List<EventOverviewDto> events;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean lastPage;

    public EventOverviewPageDto(Page<Event> eventPage) {
        this.events = eventPage.getContent().stream().map(EventOverviewDto::new).toList();
        this.pageNumber = eventPage.getNumber();
        this.pageSize = eventPage.getSize();
        this.totalElements = eventPage.getTotalElements();
        this.totalPages = eventPage.getTotalPages();
        this.lastPage = eventPage.isLast();
    }

}
